package net.statemesh.k8s.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.domain.enumeration.*;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.*;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.K8RoleRulesDTO;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.k8s.util.ObjectUtil;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.*;
import net.statemesh.service.k8s.status.NodeStatusService;
import net.statemesh.service.util.GpuUtil;
import net.statemesh.service.util.ProfileUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.FutureUtils;

import java.util.*;
import java.util.concurrent.*;

import static net.statemesh.config.Constants.*;
import static net.statemesh.config.K8Timeouts.*;
import static net.statemesh.k8s.util.K8SConstants.DENSEMAX_IMAGE;

@Component
@Slf4j
public class CreateModelFlow extends BaseApplicationFlow {
    protected final ObjectMapper objectMapper;
    protected final NodeStatusService nodeStatusService;

    public CreateModelFlow(KubernetesController kubernetesController,
                           ClusterService clusterService,
                           ResourceService resourceService,
                           ObjectMapper objectMapper,
                           NodeStatusService nodeStatusService,
                           @Qualifier("flowScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(kubernetesController, clusterService, resourceService, taskScheduler);
        this.objectMapper = objectMapper;
        this.nodeStatusService = nodeStatusService;
    }

    @Override
    public TaskResult<String> execute(ApplicationDTO rootApplication) throws K8SException {
        final var modelConfig = getModelConfig(rootApplication, objectMapper);
        final var cluster = setCluster(rootApplication, kubernetesController, clusterService);

        // Ensure the namespace exists before proceeding with the deployment
        // for deploying pre-requisites like service accounts, roles, etc.
        ensureNamespace(rootApplication, cluster);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var routerDeployment = FutureUtils.callAsync(() -> {
                ensureRouterServiceAccount(rootApplication, cluster);
                return executeCreate(createRouterApplication(modelConfig, rootApplication));
            }, executor);

            var workerDeployment = FutureUtils.callAsync(() -> executeCreate(createWorkerApplication(modelConfig, rootApplication, cluster)), executor);
            var cacheDeployment = modelConfig.cachingEnabled() ?
                FutureUtils.callAsync(() -> executeCreate(createCacheApplication(rootApplication)), executor) :
                CompletableFuture.completedFuture(TaskResult.success());

            try {
                CompletableFuture.allOf(routerDeployment, workerDeployment, cacheDeployment)
                    .get(CREATE_FLOW_TIMEOUT_SECONDS * 3, TimeUnit.SECONDS);

                var routerResult = routerDeployment.get();
                var workerResult = workerDeployment.get();
                var cacheResult = cacheDeployment.get();

                if (routerResult.isFailed() || workerResult.isFailed() || cacheResult.isFailed()) {
                    log.error("One or more deployments failed: Router: {}, Worker: {}, Cache: {}",
                        routerResult.isSuccess(), workerResult.isSuccess(), cacheResult.isSuccess());
                    cleanup(rootApplication, cluster).get(DELETE_NAMESPACE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    return TaskResult.<String>fail().value(routerResult.getValue()).cluster(cluster);
                }

                if (routerResult.isWaitTimeout() || workerResult.isWaitTimeout() || cacheResult.isWaitTimeout()) {
                    log.warn("One or more deployments timed out: Router: {}, Worker: {}, Cache: {}",
                        routerResult.isWaitTimeout(), workerResult.isWaitTimeout(), cacheResult.isWaitTimeout());
                    return TaskResult.<String>waitTimeout().value(routerResult.getValue()).cluster(cluster);
                }

                return TaskResult.<String>success()
                    .value(routerResult.getValue()) // set ingress hostname from router
                    .cluster(routerResult.getCluster());
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("Error during deployment execution: {}", e.getCause().getMessage());
                cleanup(rootApplication, cluster);
                return TaskResult.fail();
            }
        }
    }

    protected ApplicationDTO createRouterApplication(ModelConfigDTO modelConfig, ApplicationDTO rootApplication) {
        var app = scaffoldRouterApplication(rootApplication, objectMapper);
        var container = app.getContainers().iterator().next();

        var requiredStartParams = new ArrayList<>(List.of(
            "--host", "0.0.0.0",
            "--port", "8000", // port 8000 must be included in the container
            "--service-discovery", "k8s",
            "--k8s-namespace", getNamespace(rootApplication),
            "--k8s-label-selector", "llmWorker=true",
            "--routing-logic", toRoutingLogic(modelConfig.getRoutingStrategy()),
            "--engine-stats-interval", "15", // Interval in seconds to scrape the serving engine metrics
            "--request-stats-window", "60" // Window size in seconds to calculate the request statistics
        ));
        if (modelConfig.getRouterReplicas() > 1 && !ObjectUtils.isEmpty(modelConfig.getRouterSessionKey())) {
            requiredStartParams.addAll(List.of("--session-key", modelConfig.getRouterSessionKey()));
        }
        if (modelConfig.cachingEnabled()) {
            requiredStartParams.addAll(List.of("--lmcache-controller-port", "9000"));
        }

        if (!ObjectUtils.isEmpty(container.getStartParameters())) {
            container.setStartParameters(
                StringUtils.join(requiredStartParams, ",") + "," + container.getStartParameters());
        } else {
            container.setStartParameters(StringUtils.join(requiredStartParams, ","));
        }

        return app;
    }

    public static ApplicationDTO scaffoldRouterApplication(
        ApplicationDTO rootApplication,
        ObjectMapper objectMapper
    ) {
        final var modelConfig = getModelConfig(rootApplication, objectMapper);
        var container = rootApplication.getContainers().stream()
            .filter(c -> MODEL_COMPONENT_ROUTER.equals(c.getDisplayName())) // Assuming the container is named "Router" in the template
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Container not found"));

        // clone the container to avoid modifying the original
        container = ObjectUtil.clone(container, ContainerDTO.class, objectMapper);

        // validate settings
        if (container.getPorts().stream().noneMatch(p -> 8000 == p.getContainerPort())) {
            throw new IllegalArgumentException("Router container must expose port 8000.");
        }

        return ApplicationDTO.builder()
            .name(rootApplication.getName() + "-router")
            .internalName(rootApplication.getInternalName() + "-router")
            .project(rootApplication.getProject())
            .replicas(modelConfig.getRouterReplicas())
            .updateStrategy(UpdateStrategy.ROLLING)
            .workloadType(WorkloadType.DEPLOYMENT)
            .containers(Set.of(container))
            .labels(Set.of(LabelDTO.builder().key("llmRouter").value("true").build()))
            .serviceAccount(routerServiceAccountName(rootApplication))
            .deployedNamespace(rootApplication.getDeployedNamespace())
            .status(rootApplication.getStatus())
            .build();
    }

    protected ApplicationDTO createWorkerApplication(
        ModelConfigDTO modelConfig, ApplicationDTO rootApplication, ClusterDTO cluster
    ) {
        var app = scaffoldWorkerApplication(rootApplication, objectMapper);
        var container = app.getContainers().iterator().next();

        final Set<NodeStatsDTO> nodeStats;
        try {
            nodeStats = nodeStatusService.statusSnapshotForNodes(cluster.getCid());
        } catch (Exception e) {
            log.error("Error reading node status: {}", e.getCause().getMessage());
            throw new RuntimeException(e);
        }

        if (nodeStats.isEmpty()) {
            throw new IllegalStateException("No node stats available to determine GPU memory.");
        }

        var availableGpuMemory = GpuUtil.availableGpuMemoryFromNodeStats(nodeStats.iterator().next());
        var requestedGpuMemory = modelConfig.getGpuMemory();

        double uniformGpuMemoryUtilization = Arrays.stream(availableGpuMemory)
            .filter(avail -> avail > 0)
            .mapToDouble(avail -> (double) requestedGpuMemory / avail)
            // Cap per-GPU utilization at 0.9 so the aggregated value never exceeds it
            .map(ratio -> Math.min(ratio, 0.9))
            .min()
            .orElse(0.0);


        final String hubSourceModel = "/models/" + (StringUtils.isEmpty(modelConfig.getLoraSourceModel()) ?
            modelConfig.getBranchToDeploy() : modelConfig.getLoraSourceModel());

        var vllmParams = new ArrayList<>(List.of(
            "serve", "hub".equals(modelConfig.getSource()) ? hubSourceModel : modelConfig.getHfModelName(),
            "--host", "0.0.0.0",
            "--port", "8000",
            "--max-model-len", modelConfig.getMaxContextSize().toString(),
            "--enable-prefix-caching",
            "--enable-chunked-prefill",
            "--trust-remote-code",
            "--gpu_memory_utilization", String.format("%.2f", uniformGpuMemoryUtilization)
        ));

        if (!StringUtils.isEmpty(modelConfig.getLoraSourceModel())) {
            vllmParams.addAll(List.of(
                "--enable-lora",
                "--max-lora-rank", "256",
                "--lora-modules", "serve-lora=/models/" + modelConfig.getBranchToDeploy()
            ));
        }
        if ("hub".equals(modelConfig.getSource())) {
            app.getContainers().add(
                StringUtils.isEmpty(modelConfig.getLoraSourceModel()) ?
                    createModelDownloadInitContainer(modelConfig.getBranchToDeploy(), null) :
                    createModelDownloadInitContainer(modelConfig.getLoraSourceModel(), modelConfig.getBranchToDeploy())
            );
        }

        if (ProfileUtil.isDevelopment(kubernetesController.getEnvironment())) {
            // Disable torch CUDA graph compilation in development mode to avoid memory issues
            vllmParams.add("--enforce-eager");
        }

        if (modelConfig.getEnablePartitioning()) {
            vllmParams.addAll(List.of("--tensor-parallel-size", modelConfig.getPartitions().toString()));
        }

        if (modelConfig.cachingEnabled()) {
            if (org.springframework.util.ObjectUtils.isEmpty(modelConfig.getDeploymentMode())
                || ModelDeploymentMode.AGGREGATED.equals(modelConfig.getDeploymentMode())) {
                vllmParams.addAll(List.of("--kv-transfer-config", "{\"kv_connector\":\"LMCacheConnectorV1\" \\, \"kv_role\":\"kv_both\"}"));

                container.getEnvVars().add(EnvironmentVariableDTO.builder().key("LMCACHE_USE_EXPERIMENTAL").value("True").build());
                container.getEnvVars().add(EnvironmentVariableDTO.builder().key("VLLM_RPC_TIMEOUT").value("1000000").build());
                container.getEnvVars().add(EnvironmentVariableDTO.builder().key("LMCACHE_LOCAL_CPU").value("True").build());
                container.getEnvVars().add(EnvironmentVariableDTO.builder().key("LMCACHE_REMOTE_SERDE").value("naive").build());
                container.getEnvVars().add(EnvironmentVariableDTO.builder().key("LMCACHE_REMOTE_URL").value(String.format("lm://%s:81", cacheServerServiceName(rootApplication))).build());

                if (modelConfig.getL1CacheSize() != null) {
                    container.getEnvVars().add(EnvironmentVariableDTO.builder().key("LMCACHE_MAX_LOCAL_CPU_SIZE").value(modelConfig.getL1CacheSize().toString()).build());
                }

                if (Boolean.TRUE.equals(modelConfig.getL2Cache())) {
                    container.getEnvVars().add(EnvironmentVariableDTO.builder().key("LMCACHE_LOCAL_DISK").value("True").build());
                    container.getEnvVars().add(EnvironmentVariableDTO.builder().key("LMCACHE_MAX_LOCAL_DISK_SIZE").value(modelConfig.getL2CacheSize().toString()).build());
                }
            } else {
                // for Disaggregated Prefill, we need a different configuration based on NIXL
                throw new UnsupportedOperationException("Disaggregated Prefill is not yet supported.");
            }
        }

        container.getEnvVars().add(EnvironmentVariableDTO.builder().key("HF_TOKEN").value(modelConfig.getHfToken()).build());
        container.getEnvVars().add(EnvironmentVariableDTO.builder().key("HF_HOME").value("/models").build());
        container.getEnvVars().add(EnvironmentVariableDTO.builder().key("PYTHONHASHSEED").value("0").build());
        container.getEnvVars().add(EnvironmentVariableDTO.builder().key("NVIDIA_VISIBLE_DEVICES").value("all").build());

        if (!ObjectUtils.isEmpty(container.getStartParameters())) {
            container.setStartParameters(
                StringUtils.join(vllmParams, ",") + "," + container.getStartParameters());
        } else {
            container.setStartParameters(StringUtils.join(vllmParams, ","));
        }

        if (Boolean.TRUE.equals(modelConfig.getEnableReplication()) && modelConfig.getRouterReplicas() > 1) {
            container.getVolumeMounts().add(
                VolumeMountDTO.builder()
                    .containerPath("shm")
                    .volume(VolumeDTO.builder()
                        .name("shm")
                        .type(VolumeType.SHM)
                        .size(1) // ideally 20Gi
                        .build())
                    .build()
            );
        }

        return app;
    }

    public static ApplicationDTO scaffoldWorkerApplication(ApplicationDTO rootApplication, ObjectMapper objectMapper) {
        final var modelConfig = getModelConfig(rootApplication, objectMapper);

        var container = rootApplication.getContainers().stream()
            .filter(c -> MODEL_COMPONENT_WORKER.equals(c.getDisplayName())) // Assuming the container is named "Worker" in the template
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Container not found"));

        // clone the container to avoid modifying the original
        container = ObjectUtil.clone(container, ContainerDTO.class, objectMapper);

        // validate settings
        if (container.getPorts().stream().noneMatch(p -> 8000 == p.getContainerPort())) {
            throw new IllegalArgumentException("Worker container must expose port 8000.");
        }

        return ApplicationDTO.builder()
            .name(rootApplication.getName() + "-worker")
            .internalName(rootApplication.getInternalName() + "-worker")
            .project(rootApplication.getProject())
            .replicas(modelConfig.getEnableReplication() ? modelConfig.getReplicas() : 1)
            .runtimeClass("nvidia")
            .hostIpc(Boolean.TRUE)
            .hostPid(Boolean.TRUE)
            .progressDeadline(1200)
            .updateStrategy(UpdateStrategy.ROLLING)
            .workloadType(WorkloadType.DEPLOYMENT)
            .containers(new HashSet<>(Set.of(container)))
            .labels(Set.of(LabelDTO.builder().key("llmWorker").value("true").build()))
            .deployedNamespace(rootApplication.getDeployedNamespace())
            .status(rootApplication.getStatus())
            .build();
    }

    protected ApplicationDTO createCacheApplication(ApplicationDTO rootApplication) {
        var app = scaffoldCacheApplication(rootApplication, objectMapper);
        var container = app.getContainers().iterator().next();

        var startParams = new ArrayList<>(List.of(
            "0.0.0.0", "9090"
        ));

        if (!ObjectUtils.isEmpty(container.getStartParameters())) {
            container.setStartParameters(
                StringUtils.join(startParams, ",") + "," + container.getStartParameters());
        } else {
            container.setStartParameters(StringUtils.join(startParams, ","));
        }

        return app;
    }

    public static ApplicationDTO scaffoldCacheApplication(ApplicationDTO rootApplication, ObjectMapper objectMapper) {
        var container = rootApplication.getContainers().stream()
            .filter(c -> MODEL_COMPONENT_CACHE.equals(c.getDisplayName())) // Assuming the container is named "Cache" in the template
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Container not found"));

        // clone the container to avoid modifying the original
        container = ObjectUtil.clone(container, ContainerDTO.class, objectMapper);

        // validate settings
        if (container.getPorts().stream().noneMatch(p -> 9090 == p.getContainerPort())) {
            throw new IllegalArgumentException("Cache container must expose port 9090.");
        }

        return ApplicationDTO.builder()
            .name(rootApplication.getName() + "-cache")
            .internalName(rootApplication.getInternalName() + "-cache")
            .project(rootApplication.getProject())
            .replicas(1)
            .updateStrategy(UpdateStrategy.ROLLING)
            .workloadType(WorkloadType.DEPLOYMENT)
            .containers(Set.of(container))
            .labels(Set.of(LabelDTO.builder().key("llmCache").value("true").build()))
            .deployedNamespace(rootApplication.getDeployedNamespace())
            .status(rootApplication.getStatus())
            .build();
    }

    @Retryable(retryFor = {
        RoleCreationException.class,
        RoleBindingCreationException.class,
        ServiceAccountCreationException.class,
    }, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    private void ensureRouterServiceAccount(ApplicationDTO rootApplication, ClusterDTO cluster) throws K8SException {
        var serviceAccountName = routerServiceAccountName(rootApplication);
        var roleName = rootApplication.getInternalName() + "-pod-reader";

        try {
            var saFuture = kubernetesController.createServiceAccount(getNamespace(rootApplication), cluster, serviceAccountName)
                .orTimeout(CREATE_SERVICE_ACCOUNT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var roleFuture = kubernetesController.createRole(
                getNamespace(rootApplication), cluster, roleName,
                List.of(K8RoleRulesDTO.builder()
                    .apiGroups(List.of(""))
                    .resources(List.of("pods"))
                    .verbs(List.of("get", "watch", "list", "patch"))
                    .build())
            ).orTimeout(CREATE_ROLE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            CompletableFuture
                .allOf(saFuture, roleFuture)
                .thenCompose(v -> kubernetesController.createRoleBinding(
                    getNamespace(rootApplication), cluster,
                    rootApplication.getInternalName() + "-router-access",
                    serviceAccountName, roleName)
                )
                .get(CREATE_ROLE_TIMEOUT_SECONDS + CREATE_SERVICE_ACCOUNT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            if (e instanceof ExecutionException) {
                throw new ServiceCreationException("Service Account could not be created. Flow interrupted", e);
            } else if (e instanceof InterruptedException) {
                throw new ServiceCreationException("Service Account creation was interrupted. Flow interrupted", e);
            } else {
                throw new ServiceCreationException("Service Account creation timed out. Flow interrupted", e);
            }
            // Handle exception => Status ERRORED in UI after retries (needs user action)
        }
    }

    protected CompletableFuture<TaskResult<Void>> cleanup(ApplicationDTO rootApplication, ClusterDTO cluster) {
        return this.kubernetesController.deleteNamespace(getNamespace(rootApplication), cluster)
            .orTimeout(DELETE_NAMESPACE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private @NotNull String toRoutingLogic(ModelRoutingStrategy routingStrategy) {
        if (routingStrategy == null) {
            return "roundrobin"; // Default to round-robin if no strategy is specified
        }
        return switch (routingStrategy) {
            case ROUND_ROBIN -> "roundrobin";
            case SESSION -> "session";
            case PREFIX_BASED -> "prefixaware";
            case KV_BASED -> "kvaware";
            case DISAGGREGATED_PREFILL -> "disaggregated_prefill";
        };
    }

    protected static ModelConfigDTO getModelConfig(ApplicationDTO rootApplication, ObjectMapper objectMapper) {
        try {
            var modelConfig = objectMapper.readValue(rootApplication.getExtraConfig(), ModelConfigDTO.class);
            if (modelConfig.getEnableReplication() == null) {
                modelConfig.setEnableReplication(Boolean.FALSE);
                modelConfig.setReplicas(1);
            }
            if (modelConfig.getEnablePartitioning() == null) {
                modelConfig.setEnablePartitioning(Boolean.FALSE);
                modelConfig.setPartitions(1);
            }
            if (modelConfig.getRoutingStrategy() == null) {
                modelConfig.setRoutingStrategy(ModelRoutingStrategy.ROUND_ROBIN);
            }
            if (modelConfig.getRouterReplicas() == null || modelConfig.getRouterReplicas() < 1) {
                modelConfig.setRouterReplicas(1);
            }
            if (modelConfig.getDeploymentMode() == null) {
                modelConfig.setDeploymentMode(ModelDeploymentMode.AGGREGATED);
            }
            if (modelConfig.getL1Cache() == null) {
                modelConfig.setL1Cache(Boolean.FALSE);
                modelConfig.setL1CacheSize(0);
            }
            if (modelConfig.getL2Cache() == null) {
                modelConfig.setL2Cache(Boolean.FALSE);
                modelConfig.setL2CacheSize(0);
            }
            return modelConfig;
        } catch (JsonProcessingException e) {
            log.error("Error parsing model config for application {}: {}", rootApplication.getName(), e.getMessage());
            throw new RuntimeException("Model config could not be parsed. Please check the configuration.");
        }
    }

    public static String routerServiceAccountName(ApplicationDTO rootApplication) {
        return rootApplication.getInternalName() + "-router-account";
    }

    public String cacheServerServiceName(ApplicationDTO rootApplication) {
        var cacheApplication = scaffoldCacheApplication(rootApplication, objectMapper);
        var container = cacheApplication.getContainers().iterator().next();
        var port = container.getPorts().stream().filter(p -> p.getContainerPort() == 9090)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Cache container must expose port 9090."));

        return NamingUtils.serviceName(cacheApplication.getInternalName(), port.getName());
    }

    private ContainerDTO createModelDownloadInitContainer(String baseModel, String loraAdapter) {
        return ContainerDTO.builder()
            .displayName("modelDownload")
            .type(ContainerType.INIT)
            .imageName(DENSEMAX_IMAGE)
            .pullImageMode(PullImageMode.LOCAL_FIRST)
            .cpuLimit(2d)
            .memLimit("1024")
            .startCommand("sh")
            .startParameters(String.format(
                "-c,set -euxo pipefail 2>/dev/null || set -eu;" +
                    "lakectl fs download -r lakefs://%s/ /mnt/%s; " +
                    (StringUtils.isEmpty(loraAdapter) ? "%s%s" : "lakectl fs download -r lakefs://%s/ /mnt/%s;"),
                baseModel, baseModel,
                StringUtils.isEmpty(loraAdapter) ? "" : loraAdapter, StringUtils.isEmpty(loraAdapter) ? "" : loraAdapter
            ))
            .volumeMounts(Set.of(modelsVolumeMount()))
            .envVars(Set.of(
                EnvironmentVariableDTO.builder().key(RAY_JOB_ENV_LAKECTL_SERVER_ENDPOINT_URL)
                    .value(kubernetesController.getApplicationProperties().getLakeFs().getEndpointInternal()).build(),
                EnvironmentVariableDTO.builder().key(RAY_JOB_ENV_LAKECTL_CREDENTIALS_ACCESS_KEY_ID)
                    .value(kubernetesController.getApplicationProperties().getLakeFs().getKey()).build(),
                EnvironmentVariableDTO.builder().key(RAY_JOB_ENV_LAKECTL_CREDENTIALS_SECRET_ACCESS_KEY)
                    .value(kubernetesController.getApplicationProperties().getLakeFs().getSecret()).build()
            ))
            .build();
    }

    private VolumeMountDTO modelsVolumeMount() {
        return VolumeMountDTO.builder()
            .containerPath("/mnt")
            .volume(VolumeDTO.builder()
                .name("models")
                .type(VolumeType.HOST_PATH)
                .path("/models")
                .size(100)
                .build())
            .build();
    }
}
