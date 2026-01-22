package net.statemesh.k8s.flow;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.domain.enumeration.VolumeType;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.DeploymentCreationException;
import net.statemesh.k8s.exception.IngressCreationException;
import net.statemesh.k8s.exception.ServiceCreationException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.*;

@Slf4j
public abstract class BaseDatabaseFlow extends ResourceCreationFlow<DatabaseDTO> {
    private static final Integer DATABASE_PORT = 5432;

    public BaseDatabaseFlow(KubernetesController kubernetesController,
                            ClusterService clusterService,
                            ResourceService resourceService,
                            ThreadPoolTaskScheduler taskScheduler) {
        super(kubernetesController, clusterService, resourceService, taskScheduler);
    }

    // The order of creating resources is:
    // NAMESPACE, NETWORK_POLICY, DOCKER_SECRETS, STORAGE_CLASSES, VOLUMES, DEPLOYMENT, SERVICES, INGRESS MIDDLEWARES, INGRESS

    @Override
    CompletableFuture<Void> createStorageClasses(DatabaseDTO database, ClusterDTO cluster) {
        Set<VolumeDTO> volumes = database.getVolumeMounts().stream()
            .map(VolumeMountDTO::getVolume)
            .filter(Objects::nonNull)
            .filter(v -> !VolumeType.HOST_PATH.equals(v.getType()) && !VolumeType.SHM.equals(v.getType()))
            .collect(Collectors.toSet());

        if (StringUtils.isEmpty(cluster.getRedisUrl()) && !volumes.isEmpty()) {
            log.error("Skipping creation of storage classes for {} volumes " +
                "because redis was not configured on cluster {}", volumes.size(), cluster.getCid());
            CompletableFuture.completedFuture(null);
        }

        return doCreateStorageSecrets(database, cluster, volumes)
            .thenCompose(v -> doCreateStorageClasses(database, cluster, volumes));
    }

    @Override
    CompletableFuture<TaskResult<String>> createDeployment(DatabaseDTO database, ClusterDTO cluster) {
        return kubernetesController.createPostgreSQLCluster(getNamespace(database), cluster, database, resourceService.getUserNodes(database))
            .exceptionally(e -> {
                log.error("Error creating PostgreSQL cluster {}: {}", database.getName(), e.getMessage());
                throw new DeploymentCreationException("DB cluster creation failed. Flow interrupted", e);
            });
    }

    @Override
    CompletableFuture<?> createServices(DatabaseDTO database, ClusterDTO cluster) {
        return kubernetesController.createService(
            getNamespace(database), cluster, database.getInternalName(),
            PortDTO.builder().name(DATABASE_PORT.toString()).servicePort(DATABASE_PORT).targetPort(DATABASE_PORT).build(),
            Map.of(SERVICE_SELECTOR_LABEL_CLUSTER_NAME, database.getInternalName())
        ).exceptionally(e -> {
            throw new ServiceCreationException("Services could not be ensured in time. Flow interrupted", e);
        });
    }

    @Override
    CompletableFuture<TaskResult<String>> createIngress(DatabaseDTO database, ClusterDTO cluster, Set<ResourceCreationFlow.Step> succeeded) {
        boolean wantsIngress = Optional.ofNullable(database.getHasIngress()).orElse(Boolean.FALSE);
        if (!wantsIngress) return CompletableFuture.completedFuture(TaskResult.success());

        return createMiddlewares(database, cluster, succeeded)
            .thenCompose(mws -> kubernetesController.createIngressTCP(
                getNamespace(database), cluster, database.getInternalName(),
                serviceName(database.getInternalName(), DATABASE_PORT.toString()),
                DATABASE_PORT,
                database.getIngressHostName(),
                TRAEFIK_DB_ENTRYPOINT,
                mws
            )).exceptionally(e -> {
                throw new IngressCreationException("Ingress could not be ensured in time. Flow interrupted", e);
            });
    }

    private CompletableFuture<List<String>> createMiddlewares(DatabaseDTO database, ClusterDTO cluster, Set<Step> succeeded) {
        if (database.ipAllowEntries().isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return kubernetesController.createIpAllowMiddlewareTCP(getNamespace(database), cluster, database.getInternalName(), DATABASE_PORT, database.ipAllowEntries())
            .thenApply(result -> {
                    String name = ipAllowMiddlewareName(database.getInternalName(), DATABASE_PORT.toString());
                    succeeded.add(Step.MIDDLEWARES);
                    return List.of(name);
                }
            ).exceptionally(e -> {
                throw new IngressCreationException("IP Allow Middleware could not be ensured in time. Flow interrupted", e);
            });
    }

    @Override
    CompletableFuture<Void> deleteStorageClasses(DatabaseDTO database) {
        Set<VolumeDTO> volumes = database.getVolumeMounts().stream()
            .map(VolumeMountDTO::getVolume)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (volumes.isEmpty()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(Streams.concat(
            doDeleteStorageClasses(database, volumes),
            doDeleteStorageSecrets(database, volumes)
        ).toArray(CompletableFuture[]::new));
    }

    @Override
    protected CompletableFuture<TaskResult<Void>> deleteDeployment(DatabaseDTO database) {
        return this.kubernetesController.deleteDatabase(getNamespace(database), setCluster(database, kubernetesController, clusterService), database);
    }

    @Override
    protected CompletableFuture<Void> deleteServices(DatabaseDTO database) {
        return CompletableFuture.allOf(
            kubernetesController.deleteService(getNamespace(database), setCluster(database, kubernetesController, clusterService), serviceName(database.getInternalName(), DATABASE_PORT.toString()))
        );
    }

    @Override
    protected CompletableFuture<TaskResult<Void>> deleteIngress(DatabaseDTO database) {
        return this.kubernetesController.deleteIngressTCP(getNamespace(database), setCluster(database, kubernetesController, clusterService), database.getInternalName(), DATABASE_PORT);
    }

    @Override
    protected CompletableFuture<Void> deleteMiddlewares(DatabaseDTO database) {
        List<String> middlewares = new ArrayList<>();
        if (!database.ipAllowEntries().isEmpty()) {
            middlewares.add(ipAllowMiddlewareName(database.getInternalName(), DATABASE_PORT.toString()));
        }
        return CompletableFuture.allOf(middlewares.stream().map(middleware -> kubernetesController.deleteMiddlewareTCP(getNamespace(database), setCluster(database, kubernetesController, clusterService), middleware)).toArray(CompletableFuture[]::new));
    }

    @Override
    public ClusterDTO setCluster(DatabaseDTO database, KubernetesController kubernetesController, ClusterService clusterService) {
        if (database.getProject() == null) {
            throw new RuntimeException("Database " + database.getName() + " has no project");
        }
        if (database.getProject().getCluster() != null) {
            return database.getProject().getCluster();
        }
        String clusterId = kubernetesController.selectClusterForDatabase(database.getProject().getZone().getZoneId(), database);
        if (StringUtils.isEmpty(clusterId)) {
            throw new RuntimeException("No cluster was present in zone: " + database.getProject().getZone().getZoneId());
        }
        var cluster = clusterService.findFirstByCid(clusterId).orElseThrow(() -> new RuntimeException("Cluster " + clusterId + " was not found"));
        database.getProject().setCluster(cluster);
        return cluster;
    }

    @Override
    String getNamespace(DatabaseDTO database) {
        return !StringUtils.isEmpty(database.getDeployedNamespace()) ? database.getDeployedNamespace() : database.getProject().getNamespace();
    }

    @Override
    void setPublicHostname(DatabaseDTO database, ClusterDTO cluster, KubernetesController kubernetesController) {
        if (StringUtils.isEmpty(database.getIngressHostName())) {
            database.setIngressHostName(
                publicHostname(cluster.getCid(),
                    database.getInternalName(),
                    PUBLIC_DB_INGRESS_HOSTNAME_PREFIX,
                    kubernetesController.getSystemConfigurationService().getConfig().getWebDomain())
            );
        }
    }
}
