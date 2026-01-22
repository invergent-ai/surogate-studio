package net.statemesh.k8s.task.application;

import com.google.common.collect.Streams;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Yaml;
import net.statemesh.domain.enumeration.ContainerType;
import net.statemesh.domain.enumeration.ProbeType;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.k8s.util.ObjectUtil;
import net.statemesh.service.dto.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.kubernetes.client.custom.Quantity.Format.DECIMAL_EXPONENT;
import static net.statemesh.config.Constants.DOCKER_HUB_REGISTRY_NAME;
import static net.statemesh.k8s.util.ApiUtils.replicas;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.*;
import static net.statemesh.k8s.util.ObjectUtil.safeGet;

public class DeploymentTask extends BaseMutationTask<String> {
    private final Logger log = LoggerFactory.getLogger(DeploymentTask.class);

    private final ClusterDTO cluster;
    private final ApplicationDTO application;
    private final List<String> userNodes;

    public DeploymentTask(ApiStub apiStub,
                          TaskConfig taskConfig,
                          String namespace,
                          ClusterDTO cluster,
                          ApplicationDTO application,
                          List<String> userNodes) {
        super(apiStub, taskConfig, namespace);
        this.cluster = cluster;
        this.application = application;
        this.userNodes = userNodes;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<String> taskResult) throws ApiException, SkippedExistsException {
        log.info("Deploying application {}", application.getName());

        KubernetesObject kobj = switch (application.getWorkloadType()) {
            case DEPLOYMENT -> getApiStub().getAppsV1Api().createNamespacedDeployment(
                getNamespace(),
                createDeployment()
            ).execute();
            case STATEFUL_SET -> getApiStub().getAppsV1Api().createNamespacedStatefulSet(
                getNamespace(),
                createStatefulSet()
            ).execute();
            case DAEMON_SET -> getApiStub().getAppsV1Api().createNamespacedDaemonSet(
                getNamespace(),
                createDaemonSet()
            ).execute();
        };

        taskResult.value(safeGet(kobj, () -> Yaml.dump(kobj)));
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Deployment :: {} :: wait poll step", application.getName());
        return Objects.equals(
            replicas(
                getApiStub().getAppsV1Api(),
                getNamespace(),
                application.getInternalName(),
                application.getWorkloadType()
            ).readyReplicas(),
            application.getReplicas()
        );
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<String> taskResult, boolean ready) {
        log.info("Application {} deployed successfully [{}]", application.getName(), ready);
    }

    private V1Deployment createDeployment() {
        return new V1Deployment()
            .apiVersion(APPS_API_VERSION)
            .kind(application.getWorkloadType().getValue())
            .metadata(metadata())
            .spec(
                new V1DeploymentSpec()
                    .replicas(application.getReplicas())
                    .progressDeadlineSeconds(application.getProgressDeadline())
                    .selector(selector())
                    .strategy(new V1DeploymentStrategy())
                    .template(podSpecs())
            );
    }

    private V1StatefulSet createStatefulSet() {
        return new V1StatefulSet()
            .apiVersion(APPS_API_VERSION)
            .kind(application.getWorkloadType().getValue())
            .metadata(metadata())
            .spec(
                new V1StatefulSetSpec()
                    .replicas(application.getReplicas())
                    .selector(selector())
                    .template(podSpecs())
            );
    }

    private V1DaemonSet createDaemonSet() {
        return new V1DaemonSet()
            .apiVersion(APPS_API_VERSION)
            .kind(application.getWorkloadType().getValue())
            .metadata(metadata())
            .spec(
                new V1DaemonSetSpec()
                    .selector(selector())
                    .template(podSpecs())
            );
    }

    private V1ObjectMeta metadata() {
        return new V1ObjectMeta()
            .name(application.getInternalName())
            .namespace(getNamespace())
            .labels(appLabels(application.getInternalName()));
    }

    private V1LabelSelector selector() {
        return new V1LabelSelector()
            .putMatchLabelsItem(SERVICE_SELECTOR_LABEL_NAME, application.getInternalName())
            .putMatchLabelsItem(SERVICE_SELECTOR_LABEL_INSTANCE, application.getInternalName());
    }

    private V1PodTemplateSpec podSpecs() {
        return new V1PodTemplateSpec()
            .metadata(
                new V1ObjectMeta()
                    .labels(
                        ObjectUtil.mergeMaps(
                            application.getLabels().stream()
                                .collect(Collectors.toMap(LabelDTO::getKey, LabelDTO::getValue)),
                            appLabels(application.getInternalName()))
                    )
                    .annotations(
                        application.getAnnotations().stream()
                            .collect(Collectors.toMap(AnnotationDTO::getKey, AnnotationDTO::getValue))
                    )
            )
            .spec(
                new V1PodSpec()
                    .containers(
                        application.getContainers().stream()
                            .filter(container ->
                                container.getType() == null || ContainerType.WORKER.equals(container.getType())
                            )
                            .map(this::toContainer)
                            .toList()
                    )
                    .initContainers(
                        application.getContainers().stream()
                            .filter(container -> ContainerType.INIT.equals(container.getType()))
                            .map(this::toContainer)
                            .toList()
                    )
                    .volumes(
                        application.getContainers().stream()
                            .map(ContainerDTO::getVolumeMounts)
                            .flatMap(Collection::stream)
                            .map(VolumeMountDTO::getVolume)
                            .collect(Collectors.toSet()).stream()
                            .map(this::toVolume)
                            .toList()
                    )
                    .imagePullSecrets(imagePullSecrets())
                    .affinity(podAffinity())
                    .dnsConfig(new V1PodDNSConfig().options(List.of(DNS_HACK_FIX)))
                    .serviceAccountName(application.getServiceAccount())
                    .hostIPC(application.getHostIpc())
                    .hostPID(application.getHostPid())
                    .runtimeClassName(application.getRuntimeClass())
            );
    }

    private V1Container toContainer(ContainerDTO container) {
        return new V1Container()
            .image(imageName(container))
            .name(containerName(application.getInternalName(), container.getImageName()))
            .imagePullPolicy(container.getPullImageMode().getValue())
            .env(
                Streams.concat(
                    predefinedEnvVars(),
                    container.getEnvVars().stream()
                        .map(this::toEnvVar)
                ).toList()
            )
            .command(toStartCommand(container))
            .args(toStartArgs(container))
            .ports(
                container.getPorts().stream()
                    .map(this::toPort)
                    .toList()
            )
            .resources(resourceRequirements(container))
            .livenessProbe(
                safeGet(container.getProbes(),
                    () -> toProbe(
                        container.getProbes().stream()
                            .filter(p -> p.getType().equals(ProbeType.LIVENESS))
                            .findAny()
                    )
                )
            )
            .readinessProbe(
                safeGet(container.getProbes(),
                    () -> toProbe(
                        container.getProbes().stream()
                            .filter(p -> p.getType().equals(ProbeType.READINESS))
                            .findAny()
                    )
                )
            )
            .volumeMounts(
                container.getVolumeMounts().stream()
                    .map(this::toVolumeMount)
                    .toList()
            );
    }

    private V1Probe toProbe(Optional<ProbeDTO> optProbe) {
        if (optProbe.isEmpty()) {
            return null; // No probe defined
        }

        var probe = optProbe.get();
        return new V1Probe()
            .initialDelaySeconds(probe.getInitialDelaySeconds())
            .periodSeconds(probe.getPeriodSeconds())
            .failureThreshold(probe.getFailureThreshold())
            .successThreshold(probe.getSuccessThreshold())
            .timeoutSeconds(probe.getTimeoutSeconds())
            .terminationGracePeriodSeconds(probe.getTerminationGracePeriodSeconds())
            .httpGet(safeGet(probe.getHttpPath(), () -> new V1HTTPGetAction()
                .path(probe.getHttpPath())
                .port(new IntOrString(probe.getHttpPort())))
            )
            .tcpSocket(safeGet(probe.getTcpHost(), () -> new V1TCPSocketAction()
                .host(probe.getTcpHost())
                .port(new IntOrString(probe.getTcpPort())))
            )
            .exec(probe.getExecCommand().isEmpty() ? null :
                new V1ExecAction()
                    .command(probe.getExecCommand()));
    }

    private V1Volume toVolume(VolumeDTO volume) {
        final V1Volume v1volume = new V1Volume().name(volumeName(volume.getName()));
        return switch (volume.getType()) {
            case TEMPORARY -> ephemeralVolume(volume, v1volume);
            case PERSISTENT -> persistentVolume(volume, v1volume);
            case HOST_PATH -> hostPathVolume(volume, v1volume);
            case SHM -> shmVolume(volume, v1volume);
        };
    }

    private V1Volume ephemeralVolume(VolumeDTO volume, V1Volume v1volume) {
        return v1volume.emptyDir(
            new V1EmptyDirVolumeSource()
                .sizeLimit(Quantity.fromString(volume.getSize() + VOLUME_SIZE_UNIT))
        );
    }

    private V1Volume persistentVolume(VolumeDTO volume, V1Volume v1volume) {
        return v1volume.persistentVolumeClaim(
            new V1PersistentVolumeClaimVolumeSource()
                .claimName(pvcName(volume.getName()))
        );
    }

    private V1Volume hostPathVolume(VolumeDTO volume, V1Volume v1volume) {
        return v1volume.hostPath(new V1HostPathVolumeSource()
            .path(volume.getPath())
            .type("DirectoryOrCreate")
        );
    }

    private V1Volume shmVolume(VolumeDTO volume, V1Volume v1volume) {
        return v1volume.emptyDir(new V1EmptyDirVolumeSource()
            .medium("Memory")
            .sizeLimit(Quantity.fromString(volume.getSize() + VOLUME_SIZE_UNIT))
        );
    }

    private V1EnvVar toEnvVar(EnvironmentVariableDTO environmentVariable) {
        return new V1EnvVar()
            .name(environmentVariable.getKey().trim())
            .value(environmentVariable.getValue());
    }

    private Stream<V1EnvVar> predefinedEnvVars() {
        return Stream.of(
            new V1EnvVar()
                .name(DEFAULT_ENV_VAR_INGRESS_DOMAIN)
                .value(application.getIngressHostName()),
            new V1EnvVar()
                .name(DEFAULT_ENV_VAR_NAMESPACE)
                .value(getNamespace()),
            new V1EnvVar()
                .name(DEFAULT_ENV_VAR_CLUSTER_ID)
                .value(cluster.getCid()),
            new V1EnvVar()
                .name(DEFAULT_ENV_VAR_SERVICE_FQDN)
                .value(String.format("%s.svc.%s.sm.local", getNamespace(), cluster.getCid()))
        );
    }

    private V1ContainerPort toPort(PortDTO port) {
        return new V1ContainerPort()
            .name(portNameLimit(port.getName()))
            .containerPort(port.getContainerPort())
            .protocol(port.getProtocol().getCode());
    }

    private V1VolumeMount toVolumeMount(VolumeMountDTO volumeMount) {
        return new V1VolumeMount()
            .name(volumeName(volumeMount.getVolume().getName()))
            .readOnly(volumeMount.getReadOnly())
            .mountPath(volumeMount.getContainerPath());
    }

    private List<String> toStartCommand(ContainerDTO container) {
        return !StringUtils.isEmpty(container.getStartCommand()) ?
            Collections.singletonList(container.getStartCommand()) : null;
    }

    private List<String> toStartArgs(ContainerDTO container) {
        String raw = container.getStartParameters();
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == ',') {
                addArgToken(args, current);
                continue;
            }
            current.append(c);
        }
        if (escaping) {
            current.append('\\');
        }
        addArgToken(args, current);
        return args.isEmpty() ? null : args;
    }

    private void addArgToken(List<String> args, StringBuilder sb) {
        String token = sb.toString().trim();
        sb.setLength(0);
        if (!token.isEmpty()) {
            args.add(token);
        }
    }

    private List<V1LocalObjectReference> imagePullSecrets() {
        return application.getContainers().stream()
            .filter(container -> !StringUtils.isEmpty(container.getRegistryUrl()))
            .map(ContainerDTO::getImageName)
            .filter(Objects::nonNull)
            .map(imageName -> containerName(application.getInternalName(), imageName))
            .map(NamingUtils::dockerSecretName)
            .collect(Collectors.toSet())
            .stream()
            .map(secretName -> new V1LocalObjectReference().name(secretName))
            .toList();
    }

    private V1ResourceRequirements resourceRequirements(ContainerDTO container) {
        var requirements = new V1ResourceRequirements()
            .putRequestsItem(CPU_METRIC_KEY,
                new Quantity(cpuRequest(container.getCpuLimit()), DECIMAL_EXPONENT))
            .putRequestsItem(MEMORY_METRIC_KEY,
                new Quantity(memoryRequest(container.getMemLimit()), DECIMAL_EXPONENT))
            .putLimitsItem(CPU_METRIC_KEY,
                new Quantity(BigDecimal.valueOf(container.getCpuLimit()), DECIMAL_EXPONENT))
            .putLimitsItem(MEMORY_METRIC_KEY,
                Quantity.fromString(container.memLimit()));

        if (!ObjectUtils.isEmpty(container.getGpuLimit())) {
            requirements.putLimitsItem(GPU_METRIC_KEY,
                new Quantity(BigDecimal.valueOf(container.getGpuLimit()), DECIMAL_EXPONENT));
        }

        return requirements;
    }

    private V1Affinity podAffinity() {
        final V1NodeSelector requiredAffinitySelector = new V1NodeSelector();

        V1NodeSelectorRequirement deploymentProfile = deploymentProfile();
        if (deploymentProfile != null) {
            if (requiredAffinitySelector.getNodeSelectorTerms().isEmpty()) {
                requiredAffinitySelector.addNodeSelectorTermsItem(new V1NodeSelectorTerm());
            }
            requiredAffinitySelector.getNodeSelectorTerms().getFirst().addMatchExpressionsItem(deploymentProfile);
        }

        return new V1Affinity()
            .nodeAffinity(new V1NodeAffinity()
                .requiredDuringSchedulingIgnoredDuringExecution(requiredAffinitySelector)
            );
    }

    private V1NodeSelectorRequirement notOnMasterNodes() {
        return new V1NodeSelectorRequirement()
            .key(METADATA_NODE_IS_MASTER)
            .operator(OPERATOR_NOTIN)
            .addValuesItem(Boolean.TRUE.toString());
    }

    private V1NodeSelectorRequirement freeTierNodes(boolean appIsFree) {
        return new V1NodeSelectorRequirement()
            .key(NODE_PRICE_LABEL)
            .operator(appIsFree ? OPERATOR_IN : OPERATOR_NOTIN)
            .addValuesItem("0.0");
    }

    private V1NodeSelectorRequirement deploymentProfile() {
        if (application.getProject().getProfile() == null) {
            return null;
        }
        return switch (application.getProject().getProfile()) {
            case GPU -> gpuProfile();
            case HPC -> hpcProfile();
            case MYNODE -> userNodesProfile();
            case CLOUD -> cloudProfile();
            case EDGE -> edgeProfile();
            default -> null; // HYBRID and null profiles go without restrictions
        };
    }

    private V1NodeSelectorRequirement gpuProfile() {
        return new V1NodeSelectorRequirement()
            .key(HAS_GPU_LABEL)
            .operator(OPERATOR_EXISTS);
    }

    private V1NodeSelectorRequirement hpcProfile() {
        if (StringUtils.isEmpty(application.getProject().getDatacenterName())) {
            return null;
        }
        return new V1NodeSelectorRequirement()
            .key(DATACENTER_NAME_LABEL)
            .operator(OPERATOR_IN)
            .addValuesItem(application.getProject().getDatacenterName());
    }

    private V1NodeSelectorRequirement userNodesProfile() {
        return new V1NodeSelectorRequirement()
            .key(METADATA_SMID_KEY)
            .operator(OPERATOR_IN)
            .values(userNodes);
    }

    private V1NodeSelectorRequirement cloudProfile() {
        return new V1NodeSelectorRequirement()
            .key(EDGE_NODE_LABEL)
            .operator(OPERATOR_DOESNOTEXIST);
    }

    private V1NodeSelectorRequirement edgeProfile() {
        return new V1NodeSelectorRequirement()
            .key(EDGE_NODE_LABEL)
            .operator(OPERATOR_EXISTS);
    }

    private BigDecimal cpuRequest(Double cpuLimit) {
        final double coefficient = cluster.getRequestVsLimitsCoefficientCpu() != null ?
            cluster.getRequestVsLimitsCoefficientCpu() :
            getTaskConfig().requestVsLimitsCoefficientCpu();
        return BigDecimal.valueOf(coefficient).multiply(
            BigDecimal.valueOf(cpuLimit)
        );
    }

    private BigDecimal memoryRequest(String memoryLimit) {
        final double coefficient = cluster.getRequestVsLimitsCoefficientMemory() != null ?
            cluster.getRequestVsLimitsCoefficientMemory() :
            getTaskConfig().requestVsLimitsCoefficientMemory();
        return BigDecimal.valueOf(coefficient).multiply(
            Quantity.fromString(memoryLimit).getNumber()
        );
    }

    private String imageName(ContainerDTO containerDTO) {
        return StringUtils.join(
            StringUtils.isEmpty(containerDTO.getRegistryUrl()) ? "" :
                ensureTrailingSlash(
                    isCustomRegistry(containerDTO.getRegistryUrl()) ?
                        removeProtocol(containerDTO.getRegistryUrl()) : DOCKER_HUB_REGISTRY_NAME
                ),
            containerDTO.getImageName(),
            StringUtils.isEmpty(containerDTO.getImageTag()) ? "" :
                ":" + containerDTO.getImageTag()
        );
    }

    private String ensureTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    private String removeProtocol(String url) {
        return url.startsWith("http://") ? url.substring(7) :
            url.startsWith("https://") ? url.substring(8) : url;
    }
}
