package net.statemesh.k8s.flow;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.domain.enumeration.Profile;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.*;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.dto.ResourceDTO;
import net.statemesh.service.dto.VolumeDTO;
import net.statemesh.service.util.ProfileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.statemesh.config.K8Timeouts.*;
import static net.statemesh.k8s.util.ApiUtils.createStorageSecret;
import static net.statemesh.k8s.util.AsyncRetry.retryAsync;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.storageSecretName;

@Slf4j
public abstract class ResourceCreationFlow<T extends ResourceDTO> extends ResourceDeletionFlow<T> {
    static final int MAX_ATTEMPTS = 3;
    static final Duration RETRY_DELAY = Duration.ofSeconds(3);
    private final ThreadPoolTaskScheduler taskScheduler;

    public ResourceCreationFlow(KubernetesController kubernetesController,
                                ClusterService clusterService,
                                ResourceService resourceService,
                                @Qualifier("flowScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(kubernetesController, clusterService, resourceService);
        this.taskScheduler = taskScheduler;
    }

    public abstract TaskResult<String> execute(T resource) throws K8SException;

    /**
     * Executes the flow.
     */
    public TaskResult<String> executeCreate(T resource) throws K8SException {
        if (resource.getProject() == null || StringUtils.isEmpty(getNamespace(resource))) {
            throw new RuntimeException("Namespace must be configured!");
        }

        final ClusterDTO cluster = setCluster(resource, kubernetesController, clusterService);
        setPublicHostname(resource, cluster, kubernetesController);

        final Set<Step> succeeded = Collections.synchronizedSet(EnumSet.noneOf(Step.class));
        AtomicBoolean cancelRetries = new AtomicBoolean(false);

        // Namespace first (validated)
        var namespaceFuture = retryAsync(() -> ensureNamespace(resource, cluster),
            MAX_ATTEMPTS, RETRY_DELAY, taskScheduler, t -> unwrap(t) instanceof NamespaceCreationException, cancelRetries::get)
            .thenApply(r -> {
                if (r == null || r.isFailed())
                    throw new NamespaceCreationException("Namespace unsuccessful (null or failed TaskResult)", null);
                succeeded.add(Step.NAMESPACE);
                return r;
            });

        // Network policy after namespace (validated)
        var networkPolicyFuture = namespaceFuture.thenCompose(v ->
                retryAsync(() -> createNetworkPolicy(resource, cluster), MAX_ATTEMPTS, RETRY_DELAY, taskScheduler,
                    t -> unwrap(t) instanceof NetworkPolicyCreationException, cancelRetries::get))
            .thenApply(r -> {
                if (r == null || r.isFailed())
                    throw new NetworkPolicyCreationException("Network policy unsuccessful (null or failed TaskResult)", null);
                succeeded.add(Step.NETWORK_POLICY);
                return r;
            });

        var dockerSecretsFuture = namespaceFuture.thenCompose(v ->
                retryAsync(() -> createDockerSecrets(resource, cluster), MAX_ATTEMPTS, RETRY_DELAY, taskScheduler,
                    t -> unwrap(t) instanceof DockerSecretCreationException, cancelRetries::get))
            .thenRun(() -> succeeded.add(Step.DOCKER_SECRETS));

        var storageClassesFuture = namespaceFuture.thenCompose(v ->
                retryAsync(() -> createStorageClasses(resource, cluster), MAX_ATTEMPTS, RETRY_DELAY, taskScheduler,
                    t -> unwrap(t) instanceof StorageClassCreationException, cancelRetries::get))
            .thenRun(() -> succeeded.add(Step.STORAGE_CLASSES));

        // Volumes must wait for storage classes to avoid race conditions (PVC before SC)
        var volumesFuture = storageClassesFuture.thenCompose(v ->
                retryAsync(() -> createVolumes(resource, cluster), MAX_ATTEMPTS, RETRY_DELAY, taskScheduler,
                    t -> unwrap(t) instanceof VolumeCreationException, cancelRetries::get))
            .thenRun(() -> succeeded.add(Step.VOLUMES));

        var deploymentPrereqs = CompletableFuture.allOf(volumesFuture, dockerSecretsFuture, storageClassesFuture, networkPolicyFuture);

        var deploymentFuture = deploymentPrereqs.thenCompose(v ->
                retryAsync(() -> createDeployment(resource, cluster), MAX_ATTEMPTS, RETRY_DELAY, taskScheduler,
                    t -> unwrap(t) instanceof DeploymentCreationException, cancelRetries::get))
            .thenApply(r -> {
                if (r == null || r.isFailed()) {
                    throw new DeploymentCreationException("Deployment unsuccessful (null or failed TaskResult)", null);
                }
                succeeded.add(Step.DEPLOYMENT);
                return r;
            });

        // Services must wait for deployment
        var servicesFuture = deploymentFuture.thenCompose(v ->
                retryAsync(() -> createServices(resource, cluster), MAX_ATTEMPTS, RETRY_DELAY, taskScheduler,
                    t -> unwrap(t) instanceof ServiceCreationException, cancelRetries::get))
            .thenRun(() -> succeeded.add(Step.SERVICES));

        // Ingress must wait for services (so service endpoints exist)
        var ingressFuture = servicesFuture.thenCompose(v ->
                retryAsync(() -> createIngress(resource, cluster, succeeded), MAX_ATTEMPTS, RETRY_DELAY, taskScheduler,
                    t -> unwrap(t) instanceof IngressCreationException, cancelRetries::get))
            .thenApply(r -> {
                if (r != null && (r.isSuccess() || r.isWaitTimeout())) succeeded.add(Step.INGRESS);
                return r;
            });


        // Track all component futures for potential cancellation on failure/timeout
        List<CompletableFuture<?>> componentFutures = List.of(
            networkPolicyFuture, dockerSecretsFuture, storageClassesFuture, volumesFuture,
            servicesFuture, ingressFuture, deploymentFuture
        );

        CompletableFuture<Void> allResources = CompletableFuture.allOf(
            networkPolicyFuture, dockerSecretsFuture, storageClassesFuture, volumesFuture,
            servicesFuture, ingressFuture, deploymentFuture
        ).orTimeout(CREATE_FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            allResources.join();
            var deploymentResult = deploymentFuture.getNow(null);
            var ingressResult = ingressFuture.getNow(null);
            if (deploymentResult == null || deploymentResult.isFailed()) {
                throw new DeploymentCreationException("Deployment result invalid (null or failed)", null);
            }
            TaskResult<String> result = TaskResult.<String>from(deploymentResult).cluster(cluster);
            if (ingressResult != null && ingressResult.isSuccess() && !StringUtils.isEmpty(ingressResult.getValue())) {
                result.value(ingressResult.getValue());
            }
            return result;
        } catch (CompletionException ce) {
            cancelRetries.set(true); // signal all retry chains to abort further attempts
            Throwable root = unwrap(ce);
            // Cancel any in-flight resource futures to avoid race with cleanup
            cancelOutstanding(componentFutures);
            runCleanup(root, resource, succeeded);
            if (root instanceof K8SException k8s) throw k8s;
            throw ce;
        }
    }

    Throwable unwrap(Throwable t) {
        while (t instanceof CompletionException || t instanceof ExecutionException) {
            if (t.getCause() == null) break;
            t = t.getCause();
        }
        return t;
    }

    public CompletableFuture<TaskResult<Void>> ensureNamespace(T resource, ClusterDTO cluster) throws NamespaceCreationException {
        return kubernetesController.createNamespace(getNamespace(resource), cluster)
            .orTimeout(CREATE_NAMESPACE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .exceptionally(e -> {
                log.error("Error creating namespace for resource with message: {}", e.getMessage());
                throw new NamespaceCreationException("Namespace could not be ensured in time. Flow interrupted", e);
            });
    }

    public CompletableFuture<TaskResult<Void>> createNetworkPolicy(T resource, ClusterDTO cluster) {
        if (ProfileUtil.isCloud(kubernetesController.getEnvironment())) {
            return kubernetesController.createNetworkPolicy(getNamespace(resource), cluster)
                .orTimeout(CREATE_POLICY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("Error creating network policy: {}", e.getMessage());
                    throw new NetworkPolicyCreationException("Network policy could not be ensured in time. Flow interrupted", e);
                });
        }
        return CompletableFuture.completedFuture(TaskResult.success());
    }

    CompletableFuture<Void> createDockerSecrets(T resource, ClusterDTO cluster) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> createStorageClasses(T resource, ClusterDTO cluster) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> createVolumes(T resource, ClusterDTO cluster) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<?> createServices(T resource, ClusterDTO cluster) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<TaskResult<String>> createIngress(T resource, ClusterDTO cluster, Set<Step> succeeded) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<TaskResult<String>> createDeployment(T resource, ClusterDTO cluster) {
        return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> doCreateStorageSecrets(T resource, ClusterDTO cluster, Set<VolumeDTO> volumes) {
        log.debug("Creating {} storage secrets for resource {}", volumes.size(), resource.getName());
        return CompletableFuture.allOf(volumes.stream()
            .filter(volume -> !StringUtils.isEmpty(volume.getBucketUrl()) || !Profile.HPC.equals(resource.getProject().getProfile()))
            .map(volume -> kubernetesController.createSecret(
                    storageSecretName(volume.getName(), !StringUtils.isEmpty(volume.getBucketUrl())),
                    SECRET_TYPE_OPAQUE,
                    Map.of(SERVICE_SELECTOR_STORAGE_SECRET_TAG, SERVICE_SELECTOR_STORAGE_SECRET_TAG_VALUE),
                    createStorageSecret(volume, cluster, kubernetesController.getApi(cluster),
                        StringUtils.isEmpty(volume.getBucketUrl()) ? kubernetesController.getStorageConfig() : null),
                    getNamespace(resource), cluster)
                .orTimeout(CREATE_SECRET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            ).toArray(CompletableFuture[]::new)
        ).exceptionally(e -> {
            throw new StorageClassCreationException("Storage secrets could not be ensured in time. Flow interrupted", e);
        });
    }

    CompletableFuture<Void> doCreateStorageClasses(T resource, ClusterDTO cluster, Set<VolumeDTO> volumes) throws StorageClassCreationException {
        log.debug("Creating {} storage classes for resource {}", volumes.size(), resource.getName());
        return CompletableFuture.allOf(volumes.stream()
            .filter(volume -> !StringUtils.isEmpty(volume.getBucketUrl()) || !Profile.HPC.equals(resource.getProject().getProfile()))
            .map(volume ->
                kubernetesController.createStorageClass(getNamespace(resource), cluster, volume)
                    .orTimeout(CREATE_STORAGE_CLASS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            )
            .toArray(CompletableFuture[]::new)
        ).exceptionally(e -> {
            throw new StorageClassCreationException("Storage classes could not be ensured in time. Flow interrupted", e);
        });
    }

    void runCleanup(Throwable cause, T resource, Set<Step> succeeded) {
        Throwable root = unwrap(cause);

        // Resources that must be deleted (include failed step even if not marked succeeded)
        Set<Step> toDelete = EnumSet.copyOf(succeeded);
        if (root instanceof IngressCreationException) {
            toDelete.add(Step.INGRESS);
            toDelete.add(Step.MIDDLEWARES);
        }
        if (root instanceof DeploymentCreationException) {
            toDelete.add(Step.DEPLOYMENT);
        }

        // Deletion order (preserve original intent)
        List<Step> order = List.of(
            Step.INGRESS,
            Step.MIDDLEWARES,
            Step.DEPLOYMENT,
            Step.SERVICES,
            Step.VOLUMES,
            Step.STORAGE_CLASSES,
            Step.DOCKER_SECRETS
        );

        for (Step flowStep : order) {
            if (!toDelete.contains(flowStep)) continue;

            try {
                switch (flowStep) {
                    case INGRESS -> deleteIngress(resource).join();
                    case MIDDLEWARES -> deleteMiddlewares(resource).join();
                    case DEPLOYMENT -> deleteDeployment(resource).join();
                    case SERVICES -> deleteServices(resource).join();
                    case VOLUMES -> deleteVolumes(resource).join();
                    case STORAGE_CLASSES -> deleteStorageClasses(resource).join();
                    case DOCKER_SECRETS -> deleteDockerSecrets(resource).join();
                    default -> { /* ignore */ }
                }
            } catch (Exception e) {
                log.debug("{} deletion failed: {}", flowStep, e.getMessage());
            }
        }
    }

    void cancelOutstanding(Collection<CompletableFuture<?>> futures) {
        for (CompletableFuture<?> f : futures) {
            if (f != null && !f.isDone()) {
                boolean cancelled = f.cancel(true);
                if (cancelled && log.isDebugEnabled()) {
                    log.debug("Cancelled outstanding future: {}", f);
                }
            }
        }
    }

    abstract void setPublicHostname(T resource, ClusterDTO cluster, KubernetesController kubernetesController);
}

