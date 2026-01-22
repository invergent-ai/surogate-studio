package net.statemesh.k8s.task.control;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import net.statemesh.domain.enumeration.WorkloadType;
import net.statemesh.k8s.task.BaseTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.k8s.util.ResourceStatus;
import net.statemesh.service.dto.ApplicationDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static net.statemesh.k8s.util.K8SConstants.*;

public class AppStatusTask extends BaseTask<List<ResourceStatus>> {
    private final Logger log = LoggerFactory.getLogger(AppStatusTask.class);

    private final ApplicationDTO application;
    private final String podName;
    private final String component;

    public AppStatusTask(ApiStub apiStub,
                         TaskConfig taskConfig,
                         String namespace,
                         ApplicationDTO application,
                         String podName,
                         String component) {
        super(apiStub, taskConfig, namespace);
        this.application = application;
        this.podName = podName;
        this.component = component;
    }

    @Override
    public CompletableFuture<TaskResult<List<ResourceStatus>>> call() {
        log.trace("Get status for application {} with workload type {}",
            application.getInternalName(),
            application.getWorkloadType());

        try {
            // If specific pod requested, return only that pod's status
            if (!StringUtils.isEmpty(podName)) {
                var pod = getApiStub().getCoreV1Api().readNamespacedPod(
                    podName,
                    getNamespace()
                ).execute();

                return CompletableFuture.completedFuture(
                    TaskResult.<List<ResourceStatus>>builder()
                        .success(Boolean.TRUE)
                        .value(List.of(ResourceStatus.fromPodStatus(
                            application,
                            pod.getStatus(),
                            Objects.requireNonNull(pod.getMetadata()).getName(),
                            component)))
                        .build()
                );
            }

            WorkloadType workloadType = application.getWorkloadType() != null ?
                application.getWorkloadType() : WorkloadType.DEPLOYMENT;

            CompletableFuture<TaskResult<List<ResourceStatus>>> result;
            switch (workloadType) {
                case STATEFUL_SET -> result = handleStatefulSetStatus();
                case DAEMON_SET -> result = handleDaemonSetStatus();
                default -> result = handleDeploymentStatus();
            }

            TaskResult<List<ResourceStatus>> taskResult = result.get();
            if (taskResult.isSuccess() && taskResult.getValue().isEmpty() &&
                workloadType == WorkloadType.DEPLOYMENT) {
                log.debug("Deployment not found, trying StatefulSet for {}", application.getInternalName());
                return handleStatefulSetStatus();
            }

            return CompletableFuture.completedFuture(taskResult);

        } catch (Exception e) {
            log.error("Failed to read status for {}: {}", application.getInternalName(), e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }


    private CompletableFuture<TaskResult<List<ResourceStatus>>> handleDeploymentStatus() throws ApiException {
        var deployment = getApiStub().getAppsV1Api().listNamespacedDeployment(getNamespace()).execute()
            .getItems().stream()
            .filter(d -> application.getInternalName().equals(Objects.requireNonNull(d.getMetadata()).getName()))
            .findFirst()
            .orElse(null);

        if (deployment == null) {
            log.debug("Deployment {} not found in namespace {}", application.getInternalName(), getNamespace());
            return CompletableFuture.completedFuture(
                TaskResult.<List<ResourceStatus>>builder()
                    .success(Boolean.TRUE)
                    .value(List.of())
                    .build()
            );
        }

        if (deployment.getStatus() == null || deployment.getStatus().getReplicas() == null ||
            deployment.getStatus().getReplicas() == 0) {
            log.debug("Deployment {} is stopped (0 replicas)", application.getInternalName());
            return CompletableFuture.completedFuture(
                TaskResult.<List<ResourceStatus>>builder()
                    .success(Boolean.TRUE)
                    .value(ResourceStatus.fromStoppedDeployment(application, component))
                    .build()
            );
        }

        return getPodStatuses();
    }

    private CompletableFuture<TaskResult<List<ResourceStatus>>> handleStatefulSetStatus() throws ApiException {
        var statefulSet = getApiStub().getAppsV1Api().listNamespacedStatefulSet(getNamespace()).execute()
            .getItems().stream()
            .filter(ss -> application.getInternalName().equals(Objects.requireNonNull(ss.getMetadata()).getName()))
            .findFirst()
            .orElse(null);

        if (statefulSet == null) {
            log.debug("StatefulSet {} not found in namespace {}", application.getInternalName(), getNamespace());
            return CompletableFuture.completedFuture(
                TaskResult.<List<ResourceStatus>>builder()
                    .success(Boolean.TRUE)
                    .value(List.of())
                    .build()
            );
        }

        if (statefulSet.getStatus() == null || statefulSet.getStatus().getReplicas() == null ||
            statefulSet.getStatus().getReplicas() == 0) {
            return CompletableFuture.completedFuture(
                TaskResult.<List<ResourceStatus>>builder()
                    .success(Boolean.TRUE)
                    .value(ResourceStatus.fromStoppedStatefulSet(application, component))
                    .build()
            );
        }

        return getPodStatuses();
    }

    private CompletableFuture<TaskResult<List<ResourceStatus>>> handleDaemonSetStatus() throws ApiException {
        var daemonSet = getApiStub().getAppsV1Api().listNamespacedDaemonSet(getNamespace()).execute()
            .getItems().stream()
            .filter(ds -> application.getInternalName().equals(Objects.requireNonNull(ds.getMetadata()).getName()))
            .findFirst()
            .orElse(null);

        if (daemonSet == null) {
            log.debug("DaemonSet {} not found in namespace {}", application.getInternalName(), getNamespace());
            return CompletableFuture.completedFuture(
                TaskResult.<List<ResourceStatus>>builder()
                    .success(Boolean.TRUE)
                    .value(List.of())
                    .build()
            );
        }

        return getPodStatuses();
    }

    private CompletableFuture<TaskResult<List<ResourceStatus>>> getPodStatuses() throws ApiException {
        String[] labelSelectors = {
            POSTGRESQL_SPILO_CLUSTER_LABEL + "=" + application.getInternalName(),
            StringUtils.join(NamingUtils.labelSelector(application.getInternalName()), ","),
            POSTGRESQL_SPILO_APPLICATION_LABEL + "=" + POSTGRESQL_SPILO_APPLICATION_VALUE + "," +
                POSTGRESQL_SPILO_CLUSTER_LABEL + "=" + application.getInternalName(),
            CNPG_CLUSTER_LABEL + "=" + application.getInternalName()
        };

        var pods = new java.util.ArrayList<io.kubernetes.client.openapi.models.V1Pod>();
        String successfulSelector = null;

        // Try each label selector until we find pods
        for (String labelSelector : labelSelectors) {
            try {
                var foundPods = getApiStub().getCoreV1Api().listNamespacedPod(getNamespace())
                    .labelSelector(labelSelector)
                    .execute()
                    .getItems();

                if (!foundPods.isEmpty()) {
                    pods.addAll(foundPods);
                    successfulSelector = labelSelector;
                    break;
                }
            } catch (ApiException e) {
                log.trace("Label selector '{}' failed: {}", labelSelector, e.getMessage());
            }
        }

        if (pods.isEmpty()) {
            log.warn("No pods found for application {} with any label selector in namespace {}",
                application.getInternalName(), getNamespace());
        }

        return CompletableFuture.completedFuture(
            TaskResult.<List<ResourceStatus>>builder()
                .success(Boolean.TRUE)
                .value(
                    pods.stream()
                        .map(pod -> ResourceStatus.fromPodStatus(
                            application,
                            pod.getStatus(),
                            Objects.requireNonNull(pod.getMetadata()).getName(),
                            component))
                        .sorted(Comparator.comparing(ResourceStatus::getPodName))
                        .toList()
                )
                .build()
        );
    }
}
