package net.statemesh.k8s.flow;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.IngressCreationException;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.exception.ServiceCreationException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.dto.PortDTO;
import net.statemesh.service.dto.TaskRunDTO;
import net.statemesh.service.dto.VolumeDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.statemesh.k8s.flow.CreateRayJobFlow.VLLM_CONTROLLER_PORT;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.publicHostname;

@Component
@Slf4j
public class TaskRunFlow extends ResourceCreationFlow<TaskRunDTO> {
    public TaskRunFlow(KubernetesController kubernetesController,
                       ClusterService clusterService,
                       ResourceService resourceService,
                       @Qualifier("flowScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(kubernetesController, clusterService, resourceService, taskScheduler);
    }

    @Override
    public TaskResult<String> execute(TaskRunDTO resource) throws K8SException {
        return executeCreate(resource);
    }

    @Override
    CompletableFuture<?> createServices(TaskRunDTO taskRun, ClusterDTO cluster) {
        if (Optional.ofNullable(taskRun.getRunInTheSky()).orElse(Boolean.FALSE)) {
            // Create service for vLLM chat controller
            return kubernetesController.createService(
                getNamespace(taskRun), cluster, taskRun.getInternalName(), vllmPort(),
                Map.of(SERVICE_SELECTOR_TASK_RUN, taskRun.getInternalName())
            ).exceptionally(e -> {
                throw new ServiceCreationException("Service could not be ensured in time. Flow interrupted", e);
            });
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<TaskResult<String>> createIngress(TaskRunDTO taskRun, ClusterDTO cluster, Set<Step> succeeded) {
        if (Optional.ofNullable(taskRun.getRunInTheSky()).orElse(Boolean.FALSE)) {
            // Create ingress for vLLM chat controller
            return kubernetesController.createIngress(
                getNamespace(taskRun),
                cluster,
                publicHostname(
                    cluster.getCid(),
                    taskRun.getInternalName(),
                    PUBLIC_VLLM_INGRESS_HOSTNAME_PREFIX,
                    kubernetesController.getSystemConfigurationService().getConfig().getWebDomain()
                ),
                taskRun.getInternalName(),
                vllmPort(),
                List.of()
            ).exceptionally(e -> {
                throw new IngressCreationException("Ingress could not be ensured in time. Flow interrupted", e);
            });
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<Void> createVolumes(TaskRunDTO taskRun, ClusterDTO cluster) {
        // Create work-dir PVC
        if (!StringUtils.isEmpty(taskRun.getWorkDirVolumeName())) {
            return CompletableFuture.allOf(
                createWorkDirPVC(taskRun, cluster)
            );
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<TaskResult<String>> createDeployment(TaskRunDTO taskRun, ClusterDTO cluster) {
        return kubernetesController.runTask(cluster, taskRun, getNamespace(taskRun));
    }

    private CompletableFuture<TaskResult<Void>> createWorkDirPVC(TaskRunDTO taskRun, ClusterDTO cluster) {
        return kubernetesController.createPVC(
            getNamespace(taskRun),
            cluster,
            VolumeDTO.builder()
                .name(taskRun.getWorkDirVolumeName())
                .size(300)
                .build(),
            LOCAL_PATH_STORAGE_CLASS
        ).exceptionally(e -> {
            throw new IngressCreationException("PVC could not be ensured in time. Flow interrupted", e);
        });
    }

    @Override
    void setPublicHostname(TaskRunDTO resource, ClusterDTO cluster, KubernetesController kubernetesController) {
        // do nothing
    }

    @Override
    String getNamespace(TaskRunDTO taskRun) {
        return !StringUtils.isEmpty(taskRun.getDeployedNamespace()) ?
            taskRun.getDeployedNamespace() :
            taskRun.getProject().getNamespace();
    }

    @Override
    String selectCluster(TaskRunDTO resource, KubernetesController kubernetesController) {
        return kubernetesController
            .selectClusterForTaskRun(resource.getProject().getZone().getZoneId());
    }

    private PortDTO vllmPort() {
        return PortDTO.builder()
            .name(VLLM_CONTROLLER_PORT.toString())
            .servicePort(VLLM_CONTROLLER_PORT)
            .targetPort(VLLM_CONTROLLER_PORT)
            .build();
    }
}
