package net.statemesh.k8s.flow;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.TaskRunDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.statemesh.config.K8Timeouts.DELETE_FLOW_TIMEOUT_SECONDS;
import static net.statemesh.k8s.flow.CreateRayJobFlow.VLLM_CONTROLLER_PORT;
import static net.statemesh.k8s.util.K8SConstants.DEFAULT_NAMESPACE;
import static net.statemesh.k8s.util.K8SConstants.SKY_MANAGED_SERVICE_LABEL;
import static net.statemesh.k8s.util.NamingUtils.pvcName;
import static net.statemesh.k8s.util.NamingUtils.serviceName;

@Component
@Slf4j
public class DeleteTaskRunFlow extends ResourceDeletionFlow<TaskRunDTO> {

    public DeleteTaskRunFlow(KubernetesController kubernetesController,
                             ClusterService clusterService,
                             ResourceService resourceService) {
        super(kubernetesController, clusterService, resourceService);
    }

    public TaskResult<Void> execute(TaskRunDTO resource) throws K8SException {
        try {
            kubernetesController.deleteTaskRun(
                getNamespace(resource),
                setCluster(resource, kubernetesController, clusterService),
                resource)
            .thenCompose(result -> CompletableFuture.allOf(
                deleteWorkDirPVC(resource),
                deleteService(resource),
                deleteIngress(resource)
            ))
            .get(DELETE_FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            throw new RuntimeException(ex);
        }
        return TaskResult.success();
    }

    protected CompletableFuture<TaskResult<Void>> deleteWorkDirPVC(TaskRunDTO resource) {
        if (StringUtils.isEmpty(resource.getWorkDirVolumeName())) {
            return CompletableFuture.completedFuture(null);
        }

        return this.kubernetesController.deleteVolumeClaim(
            getNamespace(resource),
            setCluster(resource, kubernetesController, clusterService),
            pvcName(resource.getWorkDirVolumeName())
        );
    }

    protected CompletableFuture<TaskResult<Void>> deleteService(TaskRunDTO resource) {
        if (!Optional.ofNullable(resource.getRunInTheSky()).orElse(Boolean.FALSE)) {
            return CompletableFuture.completedFuture(null);
        }

        cleanupSkyManagedServices(resource);
        return this.kubernetesController.deleteService(
            getNamespace(resource),
            setCluster(resource, kubernetesController, clusterService),
            serviceName(resource.getInternalName(), VLLM_CONTROLLER_PORT.toString())
        );
    }

    protected CompletableFuture<TaskResult<Void>> deleteIngress(TaskRunDTO resource) {
        if (!Optional.ofNullable(resource.getRunInTheSky()).orElse(Boolean.FALSE)) {
            return CompletableFuture.completedFuture(null);
        }

        return this.kubernetesController.deleteIngress(
            getNamespace(resource),
            setCluster(resource, kubernetesController, clusterService),
            resource,
            VLLM_CONTROLLER_PORT.toString()
        );
    }

    private void cleanupSkyManagedServices(TaskRunDTO resource) {
        if (!Optional.ofNullable(resource.getSkyToK8s()).orElse(Boolean.FALSE)) {
            return;
        }

        try {
            kubernetesController.listServices(
                    setCluster(resource, kubernetesController, clusterService),
                    DEFAULT_NAMESPACE
                ).getItems().stream()
                .filter(service -> service.getMetadata() != null)
                .filter(service -> service.getMetadata().getLabels() != null)
                .filter(service -> service.getMetadata().getLabels().containsKey(SKY_MANAGED_SERVICE_LABEL))
                .filter(service -> service.getMetadata().getLabels().get(SKY_MANAGED_SERVICE_LABEL) != null)
                .filter(service ->
                    service.getMetadata().getLabels().get(SKY_MANAGED_SERVICE_LABEL).startsWith(resource.getInternalName()))
                .forEach(service ->
                    kubernetesController.deleteService(
                        DEFAULT_NAMESPACE,
                        setCluster(resource, kubernetesController, clusterService),
                        service.getMetadata().getName()
                    )
                );
        } catch (ApiException e) {
            log.error("Could not cleanup sky managed services", e);
        }
    }

    @Override
    String getNamespace(TaskRunDTO resource) {
        return !StringUtils.isEmpty(resource.getDeployedNamespace()) ?
            resource.getDeployedNamespace() :
            resource.getProject().getNamespace();
    }

    @Override
    String selectCluster(TaskRunDTO resource, KubernetesController kubernetesController) {
        return kubernetesController.selectClusterForTaskRun(resource.getProject().getZone().getZoneId());
    }
}
