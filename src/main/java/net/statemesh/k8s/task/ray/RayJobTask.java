package net.statemesh.k8s.task.ray;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.options.CreateOptions;
import net.statemesh.domain.enumeration.PullImageMode;
import net.statemesh.domain.enumeration.RayJobType;
import net.statemesh.k8s.crd.rayjob.models.*;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.RayClusterShape;
import net.statemesh.service.dto.RayJobDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.stream.IntStream;

import static net.statemesh.config.Constants.USE_AXOLOTL_TRAINING_LIBRARY;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.pvcName;

public class RayJobTask extends BaseMutationTask<String> {
    private final Logger log = LoggerFactory.getLogger(RayJobTask.class);
    private final Map<RayJobType, String> entrypoints = Map.of(
        RayJobType.FINE_TUNE, "job-entry " + (USE_AXOLOTL_TRAINING_LIBRARY ? "train-axolotl train-axolotl" : "train train"),
        RayJobType.TRAIN, "job-entry " + (USE_AXOLOTL_TRAINING_LIBRARY ? "train-axolotl train-axolotl" : "train train")
    );

    private final RayJobDTO rayJob;
    private final RayClusterShape rayClusterShape;

    public RayJobTask(ApiStub apiStub,
                      TaskConfig taskConfig,
                      String namespace,
                      RayJobDTO rayJob,
                      RayClusterShape rayClusterShape) {
        super(apiStub, taskConfig, namespace);
        this.rayJob = rayJob;
        this.rayClusterShape = rayClusterShape;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<String> taskResult) throws Exception {
        log.info("Deploying ray job {}", rayJob.getName());

        if (!rayJobExists()) {
            log.debug("Create ray job {}", rayJob.getName());
            var jobSpec = rayJobSpec();
            if (EXTERNAL_RAY_CLUSTER) {
                jobSpec.clusterSelector(
                    Map.of(RAY_CLUSTER_SELECTOR_LABEL, rayJob.getProject().getRayCluster())
                );
            }
            var response = getApiStub().getRayJob().create(
                getNamespace(),
                new V1RayJob()
                    .apiVersion(RAY_GROUP + "/" + RAY_JOB_API_VERSION_V1)
                    .kind(RAY_JOB_RUN_KIND)
                    .metadata(new V1ObjectMeta().name(rayJob.getInternalName()))
                    .spec(jobSpec),
                new CreateOptions()
            );

            taskResult.value(Objects.requireNonNull(response.getObject().getMetadata()).getName());
        } else {
            log.debug("Skipping ray job {} creation as it exists", rayJob.getName());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Ray job run :: {} :: wait poll step", rayJob.getName());
        return rayJobExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<String> taskResult, boolean ready) {
        log.info("Ray job run {} created successfully [{}]", rayJob.getName(), ready);
    }

    private V1RayJobSpec rayJobSpec() {
        return new V1RayJobSpec()
            .entrypoint(entrypoints.get(rayJob.getType()))
            .shutdownAfterJobFinishes(Boolean.FALSE)
            .runtimeEnvYAML(toRuntimeEnvYaml())
            .submitterPodTemplate(
                new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplate().spec(
                    new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpec()
                        .restartPolicy("Never")
                        .dnsConfig(dnsConfig())
                        .containers(
                            Collections.singletonList(
                                new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecContainersInner()
                                    .name(RAY_JOB_SUBMITTER_CONTAINER_NAME)
                                    .image(DENSEMAX_IMAGE)
                                    .resources(
                                        new V1RayJobSpecRayClusterSpecAutoscalerOptionsResources()
                                            .requests(Map.of(CPU_METRIC_KEY, "100m", MEMORY_METRIC_KEY, "256Mi"))
                                            .limits(Map.of(CPU_METRIC_KEY, "1", MEMORY_METRIC_KEY, "1Gi"))
                                    )
                            )
                        )
                )
            )
            .rayClusterSpec(!EXTERNAL_RAY_CLUSTER ? rayClusterSpec() : null);
    }

    private V1RayJobSpecRayClusterSpec rayClusterSpec() {
        return new V1RayJobSpecRayClusterSpec()
            .rayVersion(RAY_VERSION)
            .headGroupSpec(headGroupSpec())
            .workerGroupSpecs(
                IntStream.range(0,
                        rayClusterShape.getNumNodes() + (rayClusterShape.getUseHeadAsWorker() ? -1 : 0)
                    )
                    .mapToObj(this::workerGroupSpec)
                    .toList()
            );
    }

    private V1RayJobSpecRayClusterSpecHeadGroupSpec headGroupSpec() {
        return new V1RayJobSpecRayClusterSpecHeadGroupSpec()
            .serviceType(DEFAULT_SERVICE_TYPE)
            .rayStartParams(Map.of("dashboard-host", "0.0.0.0"))
            .template(
                new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplate()
                    .metadata(
                        new V1RayJobSpecRayClusterSpecHeadGroupSpecHeadServiceMetadata()
                            .labels(Map.of(SERVICE_SELECTOR_LABEL_RAY_HEAD_SELECT, rayJob.getInternalName()))
                            .annotations(networkAnnotation())
                    )
                    .spec(
                        new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpec()
                            .containers(Collections.singletonList(
                                new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecContainersInner()
                                    .name("ray-head")
                                    .image(DENSEMAX_IMAGE)
                                    .imagePullPolicy(PullImageMode.PULL.getValue())
                                    .env(clusterEnv())
                                    .volumeMounts(volumeMounts())
                                    .resources(resourcesHead())
                            ))
                            .volumes(volumes())
                    )
            );
    }

    private V1RayJobSpecRayClusterSpecWorkerGroupSpecsInner workerGroupSpec(int idx) {
        return new V1RayJobSpecRayClusterSpecWorkerGroupSpecsInner()
            .groupName("gpu" + idx)
            .replicas(1)
            .template(
                new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplate()
                    .metadata(
                        new V1RayJobSpecRayClusterSpecHeadGroupSpecHeadServiceMetadata()
                            .annotations(networkAnnotation())
                    )
                    .spec(
                        new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpec()
                            .hostIPC(Boolean.TRUE)
                            .initContainers(initContainers())
                            .containers(containers())
                            .volumes(volumes())
                            .dnsConfig(dnsConfig())
                    )
            );
    }

    private List<V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecContainersInner> initContainers() {
        return Collections.singletonList(
          new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecContainersInner()
              .name("wait-for-gpu")
              .image(WAIT_FOR_GPU_IMAGE)
              .command(List.of("/bin/sh","-c"))
              .resources(
                  new V1RayJobSpecRayClusterSpecAutoscalerOptionsResources()
                      .requests(Map.of(GPU_RESOURCE_NAME, "1"))
                      .limits(Map.of(GPU_RESOURCE_NAME, "1"))
              )
              .args(Collections.singletonList(WAIT_FOR_GPU_SCRIPT))
        );
    }

    private List<V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecContainersInner> containers() {
        return Collections.singletonList(
            new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecContainersInner()
                .name("ray-worker")
                .image(DENSEMAX_IMAGE)
                .imagePullPolicy(PullImageMode.PULL.getValue())
                .securityContext(securityContext())
                .env(clusterEnv())
                .volumeMounts(volumeMounts())
                .resources(resources())
        );
    }

    private List<V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInner> volumes() {
        return List.of(
            new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInner()
                .name("log-volume")
                .emptyDir(new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInnerEmptyDir()),
            new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInner()
                .name("work-dir")
                .persistentVolumeClaim(
                  new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInnerPersistentVolumeClaim()
                      .claimName(
                          EXTERNAL_RAY_CLUSTER ? RAY_WORKDIR_VOLUME_NAME : pvcName(rayJob.getWorkDirVolumeName())
                      )
                ),
            new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInner()
                .name("aim")
                .nfs(
                    new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInnerNfs()
                        .server(NFS_SERVER)
                        .path(NFS_AIM_PATH)
                ),
            new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInner()
                .name("dshm")
                .hostPath(
                    new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInnerHostPath()
                        .path("/dev/shm")
                ),
            new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInner()
                .name("devinf")
                .hostPath(
                    new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecVolumesInnerHostPath()
                        .path("/dev/infiniband")
                )
        );
    }

    private List<V1RayJobSpecRayClusterSpecAutoscalerOptionsVolumeMountsInner> volumeMounts() {
        return List.of(
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsVolumeMountsInner()
                .name("log-volume")
                .mountPath("/tmp/ray"),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsVolumeMountsInner()
                .name("work-dir")
                .mountPath(RAY_WORK_DIR),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsVolumeMountsInner()
                .name("aim")
                .mountPath(AIM_DIR),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsVolumeMountsInner()
                .name("dshm")
                .mountPath("/dev/shm"),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsVolumeMountsInner()
                .name("devinf")
                .mountPath("/dev/infiniband")
        );
    }

    private V1RayJobSpecRayClusterSpecAutoscalerOptionsResources resourcesHead() {
        return new V1RayJobSpecRayClusterSpecAutoscalerOptionsResources()
            .requests(Map.of(
                SRIOV_RESOURCE_NAME, "1",
                GPU_RESOURCE_NAME, rayClusterShape.getHeadGpus().toString()))
            .limits(Map.of(
                SRIOV_RESOURCE_NAME, "1",
                GPU_RESOURCE_NAME, rayClusterShape.getHeadGpus().toString()));
    }

    private V1RayJobSpecRayClusterSpecAutoscalerOptionsResources resources() {
        return new V1RayJobSpecRayClusterSpecAutoscalerOptionsResources()
            .requests(Map.of(
                SRIOV_RESOURCE_NAME, "1",
                GPU_RESOURCE_NAME, rayClusterShape.getGpusPerWorker().toString()))
            .limits(Map.of(
                SRIOV_RESOURCE_NAME, "1",
                GPU_RESOURCE_NAME, rayClusterShape.getGpusPerWorker().toString()));
    }

    private List<V1RayJobSpecRayClusterSpecAutoscalerOptionsEnvInner> clusterEnv() {
        return List.of(
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsEnvInner()
                .name("NVIDIA_VISIBLE_DEVICES")
                .value("all"),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsEnvInner()
                .name("NCCL_IB_DISABLE")
                .value("0"),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsEnvInner()
                .name("NCCL_P2P_DISABLE")
                .value("1"),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsEnvInner()
                .name("NCCL_SHM_DISABLE")
                .value("0"),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsEnvInner()
                .name("NCCL_DEBUG")
                .value("info"),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsEnvInner()
                .name("NCCL_DEBUG_SUBSYS")
                .value("INIT,NET,ENV"),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsEnvInner()
                .name("NCCL_SOCKET_IFNAME")
                .value("net1"),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsEnvInner()
                .name("NCCL_IB_HCA")
                .value("mlx5_1,mlx5_2,mlx5_3,mlx5_4,mlx5_5,mlx5_6,mlx5_7"),
            new V1RayJobSpecRayClusterSpecAutoscalerOptionsEnvInner()
                .name("NCCL_IB_PORT")
                .value("1")
        );
    }

    private Map<String, String> networkAnnotation() {
        return Map.of(NAD_SELECTOR_ANNOTATION, SRIOV_NAD_NAME);
    }

    private V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecDnsConfig dnsConfig() {
        return new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecDnsConfig()
            .options(
                Collections.singletonList(
                    new V1RayJobSpecRayClusterSpecHeadGroupSpecTemplateSpecDnsConfigOptionsInner()
                        .name("ndots")
                        .value("1")
                )
            );
    }

    private V1RayJobSpecRayClusterSpecAutoscalerOptionsSecurityContext securityContext() {
        return new V1RayJobSpecRayClusterSpecAutoscalerOptionsSecurityContext()
            .privileged(Boolean.TRUE)
            .capabilities(
                new V1RayJobSpecRayClusterSpecAutoscalerOptionsSecurityContextCapabilities()
                    .add(Collections.singletonList("IPC_LOCK"))
            );
    }

    private String toRuntimeEnvYaml() {
        Map<String, Object> root = new HashMap<>();
        Map<String, String> envVars = new HashMap<>();
        rayJob.getEnvVars().forEach(
            envVar -> envVars.put(envVar.getKey(), envVar.getValue()));
        root.put("env_vars", envVars);
        root.put("py_executable",
            "/opt/densemax/" + (USE_AXOLOTL_TRAINING_LIBRARY ? "train-axolotl" : "train") + "/.venv/bin/python");

        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        return new Yaml(options).dump(root);
    }

    private boolean rayJobExists() {
        V1RayJobList jobs = getApiStub().getRayJob().list(getNamespace()).getObject();
        return jobs != null && jobs.getItems().stream()
            .anyMatch(job -> rayJob.getInternalName().equals(
                Objects.requireNonNull(job.getMetadata()).getName())
            );
    }
}
