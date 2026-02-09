package net.statemesh.k8s.flow;

import com.google.common.collect.Streams;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.domain.enumeration.Profile;
import net.statemesh.domain.enumeration.VolumeType;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.*;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.statemesh.config.K8Timeouts.*;
import static net.statemesh.k8s.util.ApiUtils.createDockerSecret;
import static net.statemesh.k8s.util.ApiUtils.getStorageClassesByDatacenter;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.*;

@Slf4j
public abstract class BaseApplicationFlow extends ResourceCreationFlow<ApplicationDTO> {
    public BaseApplicationFlow(KubernetesController kubernetesController,
                               ClusterService clusterService,
                               ResourceService resourceService,
                               ThreadPoolTaskScheduler taskScheduler) {
        super(kubernetesController, clusterService, resourceService, taskScheduler);
    }

    // The order of creating resources is:
    // NAMESPACE, NETWORK_POLICY, DOCKER_SECRETS, STORAGE_CLASSES, VOLUMES, DEPLOYMENT, SERVICES, INGRESS, MIDDLEWARES

    @Override
    CompletableFuture<Void> createDockerSecrets(ApplicationDTO application, ClusterDTO cluster) {
        Set<ContainerDTO> secretContainers = application.getContainers().stream()
            .filter(c -> !StringUtils.isEmpty(c.getRegistryUrl()) &&
                !StringUtils.isEmpty(c.getRegistryUsername()) &&
                !StringUtils.isEmpty(c.getRegistryPassword()))
            .collect(Collectors.toSet());

        if (secretContainers.isEmpty()) return CompletableFuture.completedFuture(null);

        return CompletableFuture.allOf(
            secretContainers.stream().map(container ->
                kubernetesController.createSecret(
                    dockerSecretName(containerName(application.getInternalName(), container.getImageName())),
                    SECRET_TYPE_DOCKERHUB,
                    null,
                    Map.of(SECRET_DOCKER_DATA_KEY, createDockerSecret(container.getRegistryUrl(),
                        container.getRegistryUsername(), container.getRegistryPassword())),
                    getNamespace(application), cluster
                ).orTimeout(CREATE_SECRET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            ).toArray(CompletableFuture[]::new)
        ).exceptionally(e -> {
            throw new DockerSecretCreationException("Docker secrets could not be ensured in time. Flow interrupted", e);
        });
    }

    @Override
    CompletableFuture<Void> createStorageClasses(ApplicationDTO application, ClusterDTO cluster) {
        Set<VolumeDTO> volumes = application.getContainers().stream()
            .map(ContainerDTO::getVolumeMounts).flatMap(Set::stream)
            .map(VolumeMountDTO::getVolume)
            .filter(Objects::nonNull)
            .filter(v -> !VolumeType.HOST_PATH.equals(v.getType()) && !VolumeType.SHM.equals(v.getType()))
            .collect(Collectors.toSet());

        if (volumes.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        if (StringUtils.isEmpty(cluster.getRedisUrl())) {
            log.error("Skipping storage class creation; redis not configured on cluster {}", cluster.getCid());
            return CompletableFuture.completedFuture(null);
        }

        return doCreateStorageSecrets(application, cluster, volumes)
            .thenCompose(v -> doCreateStorageClasses(application, cluster, volumes));
    }

    @Override
    CompletableFuture<Void> createVolumes(ApplicationDTO application, ClusterDTO cluster) {
        Set<VolumeDTO> persistent = application.getContainers().stream()
            .map(ContainerDTO::getVolumeMounts).flatMap(Set::stream)
            .map(VolumeMountDTO::getVolume)
            .filter(v -> v != null && VolumeType.PERSISTENT.equals(v.getType()))
            .collect(Collectors.toSet());
        if (persistent.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<String> datacenterStorageClasses;
        try {
            datacenterStorageClasses = Profile.HPC.equals(application.getProject().getProfile()) &&
                !StringUtils.isEmpty(application.getProject().getDatacenterName()) ?
                getStorageClassesByDatacenter(kubernetesController.getApi(cluster), application.getProject().getDatacenterName()) :
                Collections.emptyList();
        } catch (ApiException e) {
            return CompletableFuture.failedFuture(new VolumeCreationException("Volume claims could not be ensured in time. Flow interrupted", e));
        }

        return CompletableFuture.allOf(
            persistent.stream().map(volume -> {
                String storageClass;
                if (Profile.HPC.equals(application.getProject().getProfile()) && StringUtils.isEmpty(volume.getBucketUrl())) {
                    storageClass = datacenterStorageClasses.isEmpty() ? LOCAL_PATH_STORAGE_CLASS : datacenterStorageClasses.getFirst();
                } else if (Profile.GPU.equals(application.getProject().getProfile())) {
                    storageClass = LOCAL_PATH_STORAGE_CLASS;
                } else {
                    storageClass = storageClassName(volume.getName(), !StringUtils.isEmpty(volume.getBucketUrl()));
                }
                return kubernetesController.createPVC(getNamespace(application), cluster, volume, storageClass)
                    .orTimeout(CREATE_PVC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }).toArray(CompletableFuture[]::new)
        ).exceptionally(e -> {
            throw new VolumeCreationException("Volume claims could not be ensured in time. Flow interrupted", e);
        });
    }

    @Override
    CompletableFuture<TaskResult<String>> createDeployment(ApplicationDTO application, ClusterDTO cluster) {
        return kubernetesController.deployApplication(
                getNamespace(application),
                application,
                cluster,
                resourceService.getUserNodes(application)
            )
            .orTimeout(CREATE_DEPLOYMENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .exceptionally(e -> {
                log.error("Error creating deployment for application {}", application.getId(), e);
                throw new DeploymentCreationException("Deployment failed. Flow interrupted", e);
            });
    }

    @Override
    CompletableFuture<?> createServices(ApplicationDTO application, ClusterDTO cluster) {
        List<PortDTO> ports = application.getContainers().stream()
            .map(ContainerDTO::getPorts).flatMap(Set::stream)
            .filter(p -> p.getServicePort() != null)
            .toList();
        if (ports.isEmpty()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(
            ports.stream().map(port ->
                kubernetesController.createService(getNamespace(application), cluster, application.getInternalName(), port, null)
                    .orTimeout(CREATE_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            ).toArray(CompletableFuture[]::new)
        ).exceptionally(e -> {
            throw new ServiceCreationException("Services could not be ensured in time. Flow interrupted", e);
        });
    }

    @Override
    CompletableFuture<TaskResult<String>> createIngress(ApplicationDTO application, ClusterDTO cluster, Set<Step> succeeded) {
        Optional<PortDTO> ingressPort = ingressPort(application);
        return ingressPort.map(portDTO ->
            createMiddlewares(application, portDTO, cluster, succeeded)
                .thenCompose(mws ->
                    kubernetesController.createIngress(
                        getNamespace(application), cluster, application.getIngressHostName(),
                        application.getInternalName(), portDTO, mws
                    ).orTimeout(CREATE_INGRESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                ).exceptionally(e -> {
                    throw new IngressCreationException("Ingress could not be ensured in time. Flow interrupted", e);
                }))
            .orElseGet(() -> CompletableFuture.completedFuture(TaskResult.fail()));
    }

    private CompletableFuture<List<String>> createMiddlewares(ApplicationDTO application, PortDTO ingressPort, ClusterDTO cluster, Set<Step> succeeded) {
        if (application.ipAllowEntries().isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return kubernetesController.createIpAllowMiddleware(
                getNamespace(application), cluster, application.getInternalName(), ingressPort, application.ipAllowEntries()
            )
            .orTimeout(CREATE_INGRESS_MIDDLEWARE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .thenApply(tr -> {
                    String name = ipAllowMiddlewareName(application.getInternalName(), ingressPort.getName());
                    succeeded.add(Step.MIDDLEWARES);
                    return List.of(name);
                }
            ).exceptionally(e -> {
                throw new IngressCreationException("IP Allow Middleware could not be ensured in time. Flow interrupted", e);
            });
    }

    @Override
    CompletableFuture<Void> deleteDockerSecrets(ApplicationDTO application) {
        Set<String> secrets = application.getContainers().stream()
            .filter(c -> !StringUtils.isEmpty(c.getRegistryUrl()))
            .map(ContainerDTO::getImageName)
            .filter(Objects::nonNull)
            .map(image -> containerName(application.getInternalName(), image))
            .map(NamingUtils::dockerSecretName)
            .collect(Collectors.toSet());
        if (secrets.isEmpty()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(secrets.stream().map(name ->
                kubernetesController.deleteSecret(name, getNamespace(application), setCluster(application, kubernetesController, clusterService))
                    .orTimeout(DELETE_SECRET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            ).toArray(CompletableFuture[]::new)
        );
    }

    @Override
    CompletableFuture<Void> deleteStorageClasses(ApplicationDTO application) {
        Set<VolumeDTO> volumes = application.getContainers().stream()
            .map(ContainerDTO::getVolumeMounts).flatMap(Set::stream)
            .map(VolumeMountDTO::getVolume)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (volumes.isEmpty()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(Streams.concat(
                doDeleteStorageClasses(application, volumes),
                doDeleteStorageSecrets(application, volumes)
            ).toArray(CompletableFuture[]::new)
        );
    }

    @Override
    CompletableFuture<Void> deleteVolumes(ApplicationDTO application) {
        Set<String> pvcs = application.getContainers().stream()
            .map(ContainerDTO::getVolumeMounts).flatMap(Set::stream)
            .map(VolumeMountDTO::getVolume)
            .filter(Objects::nonNull)
            .filter(v -> !VolumeType.HOST_PATH.equals(v.getType()) && !VolumeType.SHM.equals(v.getType()))
            .map(VolumeDTO::getName)
            .map(NamingUtils::pvcName)
            .collect(Collectors.toSet());
        if (pvcs.isEmpty()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(pvcs.stream().map(pvc ->
                kubernetesController.deleteVolumeClaim(getNamespace(application), setCluster(application, kubernetesController, clusterService), pvc)
                    .orTimeout(DELETE_PVC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            ).toArray(CompletableFuture[]::new)
        );
    }

    @Override
    CompletableFuture<TaskResult<Void>> deleteDeployment(ApplicationDTO application) {
        return kubernetesController.deleteApplication(getNamespace(application), setCluster(application, kubernetesController, clusterService), application)
            .orTimeout(DELETE_DEPLOYMENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    CompletableFuture<Void> deleteServices(ApplicationDTO application) {
        List<String> services = application.getContainers().stream()
            .map(ContainerDTO::getPorts).flatMap(Set::stream)
            .filter(p -> p.getServicePort() != null)
            .map(PortDTO::getName)
            .map(n -> NamingUtils.serviceName(application.getInternalName(), n))
            .toList();
        if (services.isEmpty()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(services.stream().map(svc ->
                kubernetesController.deleteService(getNamespace(application), setCluster(application, kubernetesController, clusterService), svc)
            ).toArray(CompletableFuture[]::new)
        );
    }

    @Override
    CompletableFuture<TaskResult<Void>> deleteIngress(ApplicationDTO application) {
        return kubernetesController.deleteIngress(
            getNamespace(application), setCluster(application, kubernetesController, clusterService), application, null
        ).orTimeout(DELETE_INGRESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    CompletableFuture<Void> deleteMiddlewares(ApplicationDTO application) {
        List<String> middlewares = new ArrayList<>();
        if (!application.ipAllowEntries().isEmpty()) {
            ingressPort(application).ifPresent(port -> middlewares.add(ipAllowMiddlewareName(application.getInternalName(), port.getName())));
        }
        if (middlewares.isEmpty()) return CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(middlewares.stream().map(mw ->
                kubernetesController.deleteMiddleware(getNamespace(application), setCluster(application, kubernetesController, clusterService), mw)
                    .orTimeout(DELETE_INGRESS_MIDDLEWARE_TIMEOUT_SECONDS,  TimeUnit.SECONDS)
            ).toArray(CompletableFuture[]::new)
        );
    }

    @Override
    String getNamespace(ApplicationDTO application) {
        return !StringUtils.isEmpty(application.getDeployedNamespace()) ?
            application.getDeployedNamespace() :
            application.getProject().getNamespace();
    }

    @Override
    void setPublicHostname(ApplicationDTO application, ClusterDTO cluster, KubernetesController kubernetesController) {
        var systemConfigurationService = kubernetesController.getSystemConfigurationService();
        Optional<PortDTO> ingressPort = ingressPort(application);
        if (StringUtils.isEmpty(application.getIngressHostName()) || hasIngressHost(ingressPort)) {
            application.setIngressHostName(
                ingressPort.isPresent() && hasIngressHost(ingressPort) ?
                    ingressPort.get().getIngressHost() :
                    publicHostname(cluster.getCid(), application.getInternalName(), PUBLIC_INGRESS_HOSTNAME_PREFIX, systemConfigurationService.getConfig().getWebDomain())
            );
        } else if (!hasIngressHost(ingressPort) && !application.getIngressHostName().endsWith(systemConfigurationService.getConfig().getWebDomain())) {
            application.setIngressHostName(
                publicHostname(cluster.getCid(), application.getInternalName(), PUBLIC_INGRESS_HOSTNAME_PREFIX, systemConfigurationService.getConfig().getWebDomain())
            );
        }
    }

    private Optional<PortDTO> ingressPort(ApplicationDTO application) {
        return application.getContainers().stream()
            .map(ContainerDTO::getPorts).flatMap(Set::stream)
            .filter(p -> p.getServicePort() != null)
            .filter(p -> Boolean.TRUE.equals(p.getIngressPort()))
            .findAny();
    }

    private boolean hasIngressHost(Optional<PortDTO> ingressPort) {
        return ingressPort.isPresent() && !StringUtils.isEmpty(ingressPort.get().getIngressHost());
    }

    @Override
    String selectCluster(ApplicationDTO application, KubernetesController kubernetesController) {
        return kubernetesController
            .selectClusterForApplication(application.getProject().getZone().getZoneId(), application);
    }
}
