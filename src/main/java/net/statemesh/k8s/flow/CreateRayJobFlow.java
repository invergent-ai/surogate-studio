package net.statemesh.k8s.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.IngressCreationException;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.exception.RayJobCreationException;
import net.statemesh.k8s.exception.ServiceCreationException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static net.statemesh.config.Constants.*;
import static net.statemesh.config.K8Timeouts.*;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.publicHostname;

@Component
@Slf4j
public class CreateRayJobFlow extends ResourceCreationFlow<RayJobDTO> {
    public static final Integer VLLM_CONTROLLER_PORT = 9000;

    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;

    public CreateRayJobFlow(KubernetesController kubernetesController,
                            ClusterService clusterService,
                            ResourceService resourceService,
                            @Qualifier("flowScheduler") ThreadPoolTaskScheduler taskScheduler,
                            ApplicationProperties applicationProperties,
                            ObjectMapper objectMapper) {
        super(kubernetesController, clusterService, resourceService, taskScheduler);
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public TaskResult<String> execute(RayJobDTO rayJob) throws K8SException {
        if (rayJob.getProject() == null) {
            throw new RuntimeException("RayJob project must be present!");
        }

        final ClusterDTO cluster = setCluster(rayJob, kubernetesController, clusterService);
        TaskResult<String> result;
        String ingressHostname = null;

        ensureNamespace(rayJob, cluster);
        try {
            var rayClusterShape = objectMapper.readValue(rayJob.getRayClusterShape(), RayClusterShape.class);
            addSystemEnvVars(rayJob, rayClusterShape);

            result = this.kubernetesController.createRayJob(
                getNamespace(rayJob),
                cluster,
                rayJob,
                rayClusterShape
            ).get(CREATE_FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Create work-dir PVC
            if (!EXTERNAL_RAY_CLUSTER) {
                createPVC(rayJob, cluster).get(CREATE_PVC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            // Create service & ingress for vLLM chat controller
            final PortDTO port = PortDTO.builder()
                .name(VLLM_CONTROLLER_PORT.toString())
                .servicePort(VLLM_CONTROLLER_PORT)
                .targetPort(VLLM_CONTROLLER_PORT)
                .build();
            createServices(rayJob, cluster, port).get(CREATE_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var ingressResult = createIngress(rayJob, cluster, port).get(CREATE_INGRESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (ingressResult.isSuccess() && !StringUtils.isEmpty(ingressResult.getValue())) {
                ingressHostname = ingressResult.getValue();
            }
        } catch (ExecutionException e) {
            throw new RayJobCreationException("Ray job creation failed. Flow interrupted", e);
        } catch (InterruptedException | TimeoutException e) {
            log.warn("Could not wait until the end to ensure ray job creation: {}", e.getClass());
            result = TaskResult.waitTimeout();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
            removeSystemEnvVars(rayJob);
        }

        return result
            .value(ingressHostname != null ? ingressHostname : result.getValue())
            .cluster(cluster);
    }

    private CompletableFuture<?> createServices(RayJobDTO rayJob, ClusterDTO cluster, PortDTO port) {
        var selectors = new HashMap<String, String>();
        selectors.put(SERVICE_SELECTOR_LABEL_RAY_CLUSTER_TYPE, "head");
        if (EXTERNAL_RAY_CLUSTER) {
            selectors.put(SERVICE_SELECTOR_LABEL_RAY_CLUSTER,  rayJob.getProject().getRayCluster());
        } else {
            selectors.put(SERVICE_SELECTOR_LABEL_RAY_HEAD_SELECT, rayJob.getInternalName());
        }
        return kubernetesController.createService(
            getNamespace(rayJob), cluster, rayJob.getInternalName(), port, selectors
        ).exceptionally(e -> {
            throw new ServiceCreationException("Service could not be ensured in time. Flow interrupted", e);
        });
    }

    private CompletableFuture<TaskResult<String>> createIngress(RayJobDTO rayJob, ClusterDTO cluster, PortDTO port) {
        return kubernetesController.createIngress(
            getNamespace(rayJob),
            cluster,
            publicHostname(
                cluster.getCid(),
                rayJob.getInternalName(),
                PUBLIC_VLLM_INGRESS_HOSTNAME_PREFIX,
                kubernetesController.getSystemConfigurationService().getConfig().getWebDomain()
            ),
            rayJob.getInternalName(),
            port,
            List.of()
        ).exceptionally(e -> {
            throw new IngressCreationException("Ingress could not be ensured in time. Flow interrupted", e);
        });
    }

    private CompletableFuture<TaskResult<Void>> createPVC(RayJobDTO rayJob, ClusterDTO cluster) {
        return kubernetesController.createPVC(
            getNamespace(rayJob),
            cluster,
            VolumeDTO.builder()
                .name(rayJob.getWorkDirVolumeName())
                .size(300)
                .build(),
            RAY_NFS_STORAGE_CLASS
        ).exceptionally(e -> {
            throw new IngressCreationException("PVC could not be ensured in time. Flow interrupted", e);
        });
    }

    private void addSystemEnvVars(RayJobDTO rayJobDTO, RayClusterShape rayClusterShape) {
        rayJobDTO.getEnvVars().add(
            JobEnvironmentVariableDTO.builder()
                .key(RAY_JOB_ENV_JOB_ID)
                .value(rayJobDTO.getInternalName())
                .build()
        );
        rayJobDTO.getEnvVars().add(
            JobEnvironmentVariableDTO.builder()
                .key(RAY_JOB_ENV_WORK_DIR)
                .value(RAY_WORK_DIR)
                .build()
        );
        rayJobDTO.getEnvVars().add(
            JobEnvironmentVariableDTO.builder()
                .key(RAY_JOB_ENV_VIRTUAL_ENV)
                .value(RAY_TRAIN_VENV)
                .build()
        );
        rayJobDTO.getEnvVars().add(
            JobEnvironmentVariableDTO.builder()
                .key(RAY_JOB_ENV_LAKECTL_SERVER_ENDPOINT_URL)
                .value(applicationProperties.getLakeFs().getEndpointInternal())
                .build()
        );
        rayJobDTO.getEnvVars().add(
            JobEnvironmentVariableDTO.builder()
                .key(RAY_JOB_ENV_LAKECTL_CREDENTIALS_ACCESS_KEY_ID)
                .value(applicationProperties.getLakeFs().getKey())
                .build()
        );
        rayJobDTO.getEnvVars().add(
            JobEnvironmentVariableDTO.builder()
                .key(RAY_JOB_ENV_LAKECTL_CREDENTIALS_SECRET_ACCESS_KEY)
                .value(applicationProperties.getLakeFs().getSecret())
                .build()
        );
        rayJobDTO.getEnvVars().add(
            JobEnvironmentVariableDTO.builder()
                .key(RAY_JOB_ENV_VLLM_TP)
                .value(rayClusterShape.getTestVllmTp() + "")
                .build()
        );
        rayJobDTO.getEnvVars().add(
            JobEnvironmentVariableDTO.builder()
                .key(RAY_JOB_ENV_AXOLOTL_CONFIG)
                .value(rayJobDTO.getTrainingConfig())
                .build()
        );
    }

    private void removeSystemEnvVars(RayJobDTO rayJobDTO) {
        rayJobDTO.setEnvVars(
            rayJobDTO.getEnvVars().stream()
                .filter(envVar -> !List.of(
                    RAY_JOB_ENV_JOB_ID,
                    RAY_JOB_ENV_WORK_DIR,
                    RAY_JOB_ENV_VIRTUAL_ENV,
                    RAY_JOB_ENV_LAKECTL_SERVER_ENDPOINT_URL,
                    RAY_JOB_ENV_LAKECTL_CREDENTIALS_ACCESS_KEY_ID,
                    RAY_JOB_ENV_LAKECTL_CREDENTIALS_SECRET_ACCESS_KEY
                    ).contains(envVar.getKey())
                )
                .collect(Collectors.toSet())
        );
    }

    @Override
    void setPublicHostname(RayJobDTO resource, ClusterDTO cluster, KubernetesController kubernetesController) {
        // do nothing
    }

    @Override
    String getNamespace(RayJobDTO resource) {
        return EXTERNAL_RAY_CLUSTER ? EXTERNAL_RAY_CLUSTER_DEFAULT_NAMESPACE :
            !StringUtils.isEmpty(resource.getDeployedNamespace()) ?
                resource.getDeployedNamespace() :
                resource.getProject().getNamespace();
    }

    @Override
    String selectCluster(RayJobDTO rayJob, KubernetesController kubernetesController) {
        return kubernetesController
            .selectClusterForRayJob(rayJob.getProject().getZone().getZoneId());
    }
}
