package net.statemesh.k8s.flow;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.RayJobDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.statemesh.config.K8Timeouts.DELETE_FLOW_TIMEOUT_SECONDS;
import static net.statemesh.k8s.flow.CreateRayJobFlow.VLLM_CONTROLLER_PORT;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.pvcName;
import static net.statemesh.k8s.util.NamingUtils.serviceName;

@Component
@Slf4j
public class DeleteRayJobFlow extends ResourceDeletionFlow<RayJobDTO> {
    public DeleteRayJobFlow(KubernetesController kubernetesController,
                            ClusterService clusterService,
                            ResourceService resourceService) {
        super(kubernetesController, clusterService, resourceService);
    }

    public TaskResult<Void> execute(RayJobDTO resource) throws K8SException {
        try {
            this.kubernetesController.deleteRayJob(
                    getNamespace(resource),
                    setCluster(resource, kubernetesController, clusterService),
                    resource)
                .thenCompose(result -> CompletableFuture.allOf(
                    deleteIngress(resource),
                    deleteServices(resource),
                    deletePVC(resource)
                ))
                .get(DELETE_FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            throw new RuntimeException(ex);
        }

        return TaskResult.success();
    }

    @Override
    protected CompletableFuture<Void> deleteServices(RayJobDTO rayJob) {
        return CompletableFuture.allOf(
            kubernetesController.deleteService(
                getNamespace(rayJob),
                setCluster(rayJob, kubernetesController, clusterService),
                serviceName(rayJob.getInternalName(), VLLM_CONTROLLER_PORT.toString())
            )
        );
    }

    @Override
    protected CompletableFuture<TaskResult<Void>> deleteIngress(RayJobDTO rayJob) {
        return this.kubernetesController.deleteIngress(
            getNamespace(rayJob),
            setCluster(rayJob, kubernetesController, clusterService),
            rayJob,
            VLLM_CONTROLLER_PORT.toString()
        );
    }

    protected CompletableFuture<TaskResult<Void>> deletePVC(RayJobDTO rayJob) {
        if (EXTERNAL_RAY_CLUSTER) {
            return CompletableFuture.completedFuture(null);
        }
        return this.kubernetesController.deleteVolumeClaim(
            getNamespace(rayJob),
            setCluster(rayJob, kubernetesController, clusterService),
            pvcName(rayJob.getWorkDirVolumeName())
        );
    }

    @Override
    String getNamespace(RayJobDTO resource) {
        return EXTERNAL_RAY_CLUSTER ? EXTERNAL_RAY_CLUSTER_DEFAULT_NAMESPACE :
            !StringUtils.isEmpty(resource.getDeployedNamespace()) ?
                resource.getDeployedNamespace() :
                resource.getProject().getNamespace();
    }

    @Override
    String selectCluster(RayJobDTO resource, KubernetesController kubernetesController) {
        return kubernetesController
            .selectClusterForRayJob(resource.getProject().getZone().getZoneId());
    }
}
