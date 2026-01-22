package net.statemesh.k8s.flow;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.dto.ResourceDTO;
import net.statemesh.service.dto.VolumeDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static net.statemesh.config.K8Timeouts.*;
import static net.statemesh.k8s.util.ApiUtils.deleteRedisLogicalDatabase;

@Slf4j
public abstract class ResourceDeletionFlow<T extends ResourceDTO> {
    protected enum Step {NAMESPACE, NETWORK_POLICY, DOCKER_SECRETS, STORAGE_CLASSES, VOLUMES, DEPLOYMENT, SERVICES, INGRESS, MIDDLEWARES}

    protected final KubernetesController kubernetesController;
    protected final ClusterService clusterService;
    protected final ResourceService resourceService;

    public ResourceDeletionFlow(KubernetesController kubernetesController,
                                ClusterService clusterService,
                                ResourceService resourceService) {
        this.kubernetesController = kubernetesController;
        this.clusterService = clusterService;
        this.resourceService = resourceService;
    }

    public TaskResult<String> executeDelete(T resource) throws K8SException {
        log.info("Running delete resource flow.");

        try {
            deleteDeployment(resource)
                .thenCompose(result -> CompletableFuture.allOf(
                    deleteMiddlewares(resource),
                    deleteIngress(resource),
                    deleteServices(resource),
                    resource.isKeepVolumes() ? CompletableFuture.completedFuture(null) :
                        deleteVolumes(resource).thenCompose((unused) -> deleteStorageClasses(resource)),
                    deleteDockerSecrets(resource),
                    deleteOthers(resource)
                ))
                .get(DELETE_FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            throw new RuntimeException(ex);
        }

        return TaskResult.success();
    }

    CompletableFuture<TaskResult<Void>> deleteIngress(T resource) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> deleteMiddlewares(T resource) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<TaskResult<Void>> deleteDeployment(T resource) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> deleteServices(T resource) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> deleteVolumes(T resource) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> deleteStorageClasses(T resource) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> deleteDockerSecrets(T resource) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<TaskResult<Void>> deleteOthers(T resource) {
        return CompletableFuture.completedFuture(null);
    }

    Stream<CompletableFuture<TaskResult<Void>>> doDeleteStorageClasses(T resource, Set<VolumeDTO> volumes) {
        return volumes.stream()
            .map(v -> NamingUtils.storageClassName(v.getName(), !StringUtils.isEmpty(v.getBucketUrl())))
            .map(sc -> kubernetesController.deleteStorageClass(getNamespace(resource), setCluster(resource, kubernetesController, clusterService), sc)
                .orTimeout(DELETE_STORAGE_CLASS_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    Stream<CompletableFuture<TaskResult<Void>>> doDeleteStorageSecrets(T resource, Set<VolumeDTO> volumes) {
        return volumes.stream()
            .map(v -> NamingUtils.storageSecretName(v.getName(), !StringUtils.isEmpty(v.getBucketUrl())))
            .map(secret -> {
                ClusterDTO cluster = setCluster(resource, kubernetesController, clusterService);
                deleteRedisLogicalDatabase(kubernetesController.getApi(cluster), secret);
                return kubernetesController.deleteSecret(secret, getNamespace(resource), cluster)
                    .orTimeout(DELETE_SECRET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            });
    }

    abstract String getNamespace(T resource);

    protected ClusterDTO setCluster(T resource,
                                    KubernetesController kubernetesController,
                                    ClusterService clusterService) {
        if (resource.getProject() == null) {
            throw new RuntimeException("Resource " + resource.getName() + " has no project");
        }
        if (resource.getProject().getCluster() != null) {
            return resource.getProject().getCluster();
        }

        String clusterId = selectCluster(resource, kubernetesController);
        if (StringUtils.isEmpty(clusterId)) {
            throw new RuntimeException("No cluster was present in zone: " + resource.getProject().getZone().getZoneId());
        }

        var cluster = clusterService.findFirstByCid(clusterId)
            .orElseThrow(() -> new RuntimeException("Cluster " + clusterId + " was not found"));
        resource.getProject().setCluster(cluster);
        return cluster;
    }

    abstract String selectCluster(T resource, KubernetesController kubernetesController);
}
