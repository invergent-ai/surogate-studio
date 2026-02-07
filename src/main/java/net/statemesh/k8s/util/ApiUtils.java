package net.statemesh.k8s.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.PatchUtils;
import lombok.Builder;
import net.statemesh.domain.enumeration.NodeRole;
import net.statemesh.domain.enumeration.WorkloadType;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.dto.DatabaseDTO;
import net.statemesh.service.dto.VolumeDTO;
import okhttp3.Call;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.stream.Collectors;

import static io.kubernetes.client.extended.kubectl.Kubectl.exec;
import static net.statemesh.config.Constants.DOCKER_HUB_REGISTRY_NAME;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.folderName;
import static net.statemesh.k8s.util.NamingUtils.isCustomRegistry;

public class ApiUtils {
    private static final Logger log = LoggerFactory.getLogger(ApiUtils.class);

    @Builder
    public record Replicas(Integer replicas, Integer readyReplicas) {}
    public record AppSummary(ResourceStatus.ResourceStatusStage stage) {}
    @Builder
    private record PodSummary(String applicationName, String podName, V1PodStatus status) {}

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static NodeRole nodeRole(V1Node node) {
        return Objects.requireNonNull(Objects.requireNonNull(node.getMetadata()).getLabels())
            .containsKey(METADATA_NODE_IS_MASTER) &&
            Boolean.parseBoolean(node.getMetadata().getLabels().get(METADATA_NODE_IS_MASTER)) ?
            NodeRole.MASTER : NodeRole.NODE;
    }

    public static void deleteDanglingPods(ApiStub apiStub, String namespace, boolean deleteFinalizers) {
        deleteTerminatingPods(apiStub, namespace, deleteFinalizers);
        deleteDisruptedPods(apiStub, namespace);
    }

    public static String podName(CoreV1Api api, String namespace, String applicationName) throws ApiException {
        return podNames(api, namespace, applicationName).stream()
            .findAny()
            .orElseThrow(() -> new ApiException("No pod was found for application " + applicationName));
    }

    public static List<String> podNames(CoreV1Api api, String namespace, String resourceName) throws ApiException {
        var items = api
            .listNamespacedPod(namespace).execute()
            .getItems();
        return items.stream()
            .map(V1Pod::getMetadata)
            .filter(Objects::nonNull)
            .filter(meta -> meta.getLabels() != null)
            .filter(meta ->
                resourceName.equals(meta.getLabels().get(SERVICE_SELECTOR_LABEL_NAME))
                || resourceName.equals(meta.getLabels().get(SERVICE_SELECTOR_LABEL_CLUSTER_NAME))
                || resourceName.equals(meta.getLabels().get(SERVICE_SELECTOR_LABEL_KUBEVIRT_DOMAIN))
            )
            .map(V1ObjectMeta::getName)
            .filter(Objects::nonNull)
            .toList();
    }

    public static String rayJobPodName(CoreV1Api api, String namespace, String jobName) throws ApiException {
        var items = api
            .listNamespacedPod(namespace).execute()
            .getItems();
        return items.stream()
            .map(V1Pod::getMetadata)
            .filter(Objects::nonNull)
            .filter(meta -> meta.getLabels() != null)
            .filter(meta -> jobName.equals(meta.getLabels().get(RAY_JOB_NAME_LABEL)))
            .map(V1ObjectMeta::getName)
            .filter(Objects::nonNull)
            .findAny()
            .orElse(null);
    }

    public static List<AppSummary> podSummariesForNode(ApiStub api, String nodeName) throws ApiException {
        return getPods(api, null, nodeName).getItems().stream()
            .filter(pod -> pod.getSpec() != null && pod.getMetadata() != null)
            .filter(pod ->
                pod.getMetadata().getLabels() != null &&
                    pod.getMetadata().getLabels().containsKey(SERVICE_SELECTOR_LABEL_NAME) &&
                    pod.getMetadata().getLabels().containsKey(SERVICE_SELECTOR_TAG))
            .filter(pod -> nodeName.equals(pod.getSpec().getNodeName()))
            .map(pod ->
                PodSummary.builder()
                    .applicationName(pod.getMetadata().getLabels().get(SERVICE_SELECTOR_LABEL_NAME))
                    .podName(pod.getMetadata().getName())
                    .status(pod.getStatus())
                    .build()
            )
            .collect(Collectors.groupingBy(
                PodSummary::applicationName,
                Collectors.toSet()
            )).values().stream()
            .map(podSummaries -> podSummaries.stream()
                .findAny()
                .orElse(null)
            )
            .filter(Objects::nonNull)
            .map(ps -> ResourceStatus.fromPodStatus(null, ps.status, ps.podName))
            .map(ResourceStatus::getStage)
            .map(AppSummary::new)
            .toList();
    }

    public static Replicas replicas(AppsV1Api api, String namespace,
                                   String applicationName, WorkloadType workloadType) throws ApiException {
        Object status = switch (workloadType) {
            case DEPLOYMENT ->
                api.readNamespacedDeployment(applicationName, namespace)
                    .execute()
                    .getStatus();
            case STATEFUL_SET ->
                api.readNamespacedStatefulSet(applicationName, namespace)
                    .execute()
                    .getStatus();
            case DAEMON_SET ->
                api.readNamespacedDaemonSet(applicationName, namespace)
                    .execute()
                    .getStatus();
        };
        if (status == null) {
            throw new RuntimeException("Unexpected error occurred while retrieving replicas");
        }

        return switch (workloadType) {
            case DEPLOYMENT ->
                Replicas.builder()
                    .replicas(safeInt(((V1DeploymentStatus) status).getReplicas()))
                    .readyReplicas(safeInt(((V1DeploymentStatus) status).getReadyReplicas()))
                    .build();
            case STATEFUL_SET ->
                Replicas.builder()
                    .replicas(safeInt(((V1StatefulSetStatus) status).getReplicas()))
                    .readyReplicas(safeInt(((V1StatefulSetStatus) status).getReadyReplicas()))
                    .build();
            case DAEMON_SET ->
                Replicas.builder()
                    .replicas(safeInt(((V1DaemonSetStatus) status).getNumberAvailable()))
                    .readyReplicas(safeInt(((V1DaemonSetStatus) status).getNumberReady()))
                    .build();
        };
    }

    public static byte[] createDockerSecret(String url, String username, String password) {
        try {
            return objectMapper.writeValueAsBytes(
                DockerSecret.builder()
                    .auths(
                        Map.of(
                            isCustomRegistry(url) ? url : DOCKER_HUB_REGISTRY_NAME,
                            DockerSecret.Auth.builder()
                                .username(username)
                                .password(password)
                                .auth(Base64.getEncoder().encodeToString(
                                    StringUtils.join(username, ":", password).getBytes()
                                ))
                                .build()
                        )
                    )
                    .build()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, byte[]> createStorageSecret(VolumeDTO volume, ClusterDTO cluster,
                                                          ApiStub apiStub, StorageConfig storageConfig) {
        return Map.of(
            STORAGE_SECRET_ACCESS_KEY_KEY, storageConfig != null ?
                storageConfig.smStorageAccessKey().getBytes() : volume.getAccessKey().getBytes(),
            STORAGE_SECRET_BUCKET_KEY, storageConfig != null ?
                storageConfig.smStorageBucketUrl().getBytes( ) : volume.getBucketUrl().getBytes(),
            STORAGE_SECRET_FORMAT_OPTIONS_KEY, STORAGE_SECRET_FORMAT_OPTIONS.getBytes(),
            STORAGE_SECRET_META_URL_KEY, buildStorageMetaUrl(cluster.getRedisUrl(), apiStub).getBytes(),
            STORAGE_SECRET_FOLDER_NAME_KEY, folderName(volume.getName()).getBytes(),
            STORAGE_SECRET_SECRET_KEY_KEY, storageConfig != null ?
                storageConfig.smStorageAccessSecret().getBytes() : volume.getAccessSecret().getBytes(),
            STORAGE_SECRET_STORAGE_KEY, STORAGE_SECRET_STORAGE.getBytes()
        );
    }

    public static void deleteRedisLogicalDatabase(ApiStub api, String secretName) {
        try {
            Map<String, byte[]> data = getStorageSecretData(api, secretName);
            if (data != null && data.containsKey(STORAGE_SECRET_META_URL_KEY)) {
                final String redisUrl = new String(data.get(STORAGE_SECRET_META_URL_KEY));
                try (Jedis jedis = new Jedis(redisUrl)) {
                    jedis.flushDB();
                    log.info("Successfully flushed Redis {}", redisUrl);
                } catch (Exception e) {
                    log.warn("Could not flush Redis for secret {} at {} with message {}",
                        secretName, redisUrl, e.getMessage());
                }
            } else {
                log.warn("Storage secret {} is malformed", secretName);
            }
        } catch (ApiException e) {
            log.warn("Could not retrieve secret {} for Redis deletion", secretName);
        }
    }

    private static String buildStorageMetaUrl(String redisUrl, ApiStub apiStub) {
        return StringUtils.join(
            redisUrl,
            !redisUrl.endsWith("/") ? "/" : "",
            determineRedisLogicalDatabase(apiStub)
        );
    }

    private static int determineRedisLogicalDatabase(ApiStub apiStub) {
        try {
            List<Integer> existingIndexes =
                getStorageSecretsData(apiStub).stream()
                    .filter(data -> data.containsKey(STORAGE_SECRET_META_URL_KEY))
                    .map(data -> data.get(STORAGE_SECRET_META_URL_KEY))
                    .filter(Objects::nonNull)
                    .map(String::new)
                    .map(ApiUtils::extractLogicalDatabaseIndex)
                    .sorted()
                    .toList();
            if (existingIndexes.isEmpty()) {
                return START_REDIS_INDEX;
            }

            // This is an optimization to fill-in the gaps for deleted volumes
            // We keep this off for now because this is prone to errors when secrets are deleted manually from k8s

//            int index = START_REDIS_INDEX;
//            while (index <= existingIndexes.getLast()) {
//                if (!existingIndexes.contains(index++)) {
//                    break;
//                }
//            }
//
//            return index <= existingIndexes.getLast() ? index - 1 : index;

            return existingIndexes.getLast() + 1;
        } catch (ApiException e) {
            log.error("Could not extract Redis logical database index {}", e.getMessage());
        }
        return ERROR_REDIS_INDEX;
    }

    private static int extractLogicalDatabaseIndex(String url) {
        String[] split = url.split("/");
        try {
            return Integer.parseInt(split[split.length - 1]);
        } catch (NumberFormatException e) {
            log.error("Redis url is malformed {}", e.getMessage());
        }
        return ERROR_REDIS_INDEX;
    }

    private static List<Map<String, byte[]>> getStorageSecretsData(ApiStub apiStub) throws ApiException {
        return apiStub.getCoreV1Api().listSecretForAllNamespaces().execute()
            .getItems().stream()
            .filter(secret ->
                secret.getMetadata() != null && secret.getMetadata().getLabels() != null &&
                    secret.getMetadata().getLabels().containsKey(SERVICE_SELECTOR_STORAGE_SECRET_TAG))
            .map(V1Secret::getData)
            .filter(Objects::nonNull)
            .toList();
    }

    public static Map<String, byte[]> getStorageSecretData(ApiStub apiStub, String secretName) throws ApiException {
        return apiStub.getCoreV1Api().listSecretForAllNamespaces().execute()
            .getItems().stream()
            .filter(secret ->
                secret.getMetadata() != null && secret.getMetadata().getLabels() != null &&
                    secret.getMetadata().getLabels().containsKey(SERVICE_SELECTOR_STORAGE_SECRET_TAG))
            .filter(secret -> secretName.equals(secret.getMetadata().getName()))
            .map(V1Secret::getData)
            .filter(Objects::nonNull)
            .findAny()
            .orElse(null);
    }

    public static String readDatabasePassword(ApiStub apiStub, DatabaseDTO database) throws ApiException {
        return apiStub.getCoreV1Api().listNamespacedSecret(database.getDeployedNamespace()).execute()
            .getItems().stream()
            .filter(secret ->
                secret.getMetadata() != null && secret.getMetadata().getLabels() != null &&
                    secret.getMetadata().getLabels().containsKey(SERVICE_SELECTOR_LABEL_CLUSTER_NAME))
            .filter(secret -> database.getInternalName().equals(secret.getMetadata().getLabels().get(SERVICE_SELECTOR_LABEL_CLUSTER_NAME)))
            .map(V1Secret::getData)
            .filter(Objects::nonNull)
            .findAny()
            .filter(data -> data.containsKey(DB_SECRET_PASSWORD_KEY))
            .map(data -> new String(data.get(DB_SECRET_PASSWORD_KEY)))
            .orElse(StringUtils.EMPTY);
    }

    public static Map<String, byte[]> readSecret(ApiStub api, String namespace, String secretName) {
        try {
            V1Secret secret = api.getCoreV1Api().readNamespacedSecret(secretName, namespace).execute();
            return secret.getData();
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return null;
            }
            throw new RuntimeException("Could not read secret " + secretName, e);
        }
    }

    private static void deleteTerminatingPods(ApiStub apiStub, String namespace, boolean deleteFinalizers) {
        try {
            getPods(apiStub, namespace, null)
                .getItems().stream()
                .map(V1Pod::getMetadata)
                .filter(Objects::nonNull)
                .filter(meta -> Objects.nonNull(meta.getDeletionTimestamp()))
                .forEach(meta ->
                    forceDeletePod(apiStub, meta.getNamespace(), meta.getName(), deleteFinalizers)
                );
        } catch (ApiException e) {
            log.warn("Could not clean terminating pods for namespace {} with message {}", namespace, e.getMessage());
        }
    }

    private static void deleteDisruptedPods(ApiStub apiStub, String namespace) {
        try {
            getPods(apiStub, namespace, null)
                .getItems().stream()
                .filter(pod ->
                    pod.getStatus() != null && pod.getStatus().getConditions() != null && pod.getMetadata() != null
                )
                .filter(pod -> pod.getStatus().getConditions().stream()
                    .anyMatch(c -> "DisruptionTarget".equals(c.getType()) && "True".equals(c.getStatus()))
                )
                .forEach(pod ->
                    forceDeletePod(apiStub, pod.getMetadata().getNamespace(), pod.getMetadata().getName(), Boolean.FALSE)
                );
        } catch (ApiException e) {
            log.warn("Could not clean disrupted pods for namespace {} with message {}", namespace, e.getMessage());
        }
    }

    private static V1PodList getPods(ApiStub apiStub, String namespace, String nodeName) throws ApiException {
        return StringUtils.isEmpty(namespace) ?
            apiStub.getCoreV1Api().listPodForAllNamespaces().fieldSelector("spec.nodeName=" + nodeName).execute() :
            apiStub.getCoreV1Api().listNamespacedPod(namespace).execute();
    }

    public static V1PodList getRunningPods(ApiStub apiStub) throws ApiException {
        return apiStub.getCoreV1Api().listPodForAllNamespaces()
            .fieldSelector("status.phase=Running")
            .labelSelector(SERVICE_SELECTOR_TAG + "=" + SERVICE_SELECTOR_TAG_VALUE)
            .execute();
    }

    public static List<String> getStorageClassesByDatacenter(ApiStub apiStub, String datacenterName) throws ApiException {
        ApiResponse<V1StorageClassList> scs = apiStub.getApiClient().execute(
            apiStub.getCustomApi()
                .listClusterCustomObject(STORAGE_CLASS_GROUP, API_VERSION, STORAGE_CLASS_PLURAL)
                .buildCall(null),
            V1StorageClassList.class
        );
        return scs.getData().getItems().stream()
            .map(V1StorageClass::getMetadata)
            .filter(meta -> meta != null && meta.getLabels() != null && meta.getCreationTimestamp() != null)
            .filter(meta -> meta.getLabels().containsKey(DATACENTER_NAME_LABEL))
            .filter(meta -> datacenterName.equalsIgnoreCase(meta.getLabels().get(DATACENTER_NAME_LABEL)))
            .sorted(Comparator.comparing(V1ObjectMeta::getCreationTimestamp).reversed())
            .map(V1ObjectMeta::getName)
            .toList();
    }

    public static void executeInsidePod(ApiStub apiStub, String namespace, String podName, String container, String[] command) {
        try {
            exec()
                .apiClient(apiStub.getApiClient())
                .namespace(namespace)
                .name(podName)
                .container(container)
                .command(command)
                .execute();
        } catch (KubectlException e) {
            log.warn("Could not execute command inside pod {} in namespace {} with message {}", podName, namespace, e.getMessage());
        }
    }

    private static void forceDeletePod(ApiStub apiStub, String namespace, String podName, boolean deleteFinalizers) {
        log.debug("Deleting pod {}", podName);
        try {
            apiStub.getApiClient().execute(
                tweakCallForCoreV1Group(
                    apiStub,
                    apiStub.getCustomApi()
                        .deleteNamespacedCustomObject(DEFAULT_GROUP, API_VERSION, namespace, PODS_PLURAL, podName)
                        .gracePeriodSeconds(0)
                        .buildCall(null)
                ),
                JsonElement.class
            );
        } catch (ApiException e) {
            log.warn("Could not clean hanging pod named {} for namespace {} with message {}",
                podName, namespace, e.getMessage());
        }

        if (deleteFinalizers) {
            removeFinalizers(apiStub, namespace, podName);
        }
    }

    private static void removeFinalizers(ApiStub apiStub, String namespace, String podName) {
        try {
            PatchUtils.patch(
                V1Pod.class,
                () -> apiStub.getCoreV1Api().patchNamespacedPod(
                    podName,
                    namespace,
                    new V1Patch(DELETE_FINALIZERS_PATCH)).buildCall(null),
                V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH,
                apiStub.getApiClient()
            );
        } catch (ApiException e) {
            // This is harmless - Maybe grace period already killed the pod
            log.trace("Could not remove finalizers for pod named {} for namespace {} with message {}",
                podName, namespace, e.getMessage());
        }
    }

    private static Call tweakCallForCoreV1Group(ApiStub apiStub, Call call) {
        HttpUrl url = call.request().url();
        String basePath = apiStub.getApiClient().getBasePath();
        int offset = 0;

        for(int i = basePath.indexOf("://") + 3; i < basePath.length(); ++i) {
            if (basePath.charAt(i) == '/') {
                ++offset;
            }
        }

        HttpUrl tweakedUrl = url.newBuilder().removePathSegment(offset + 1).setPathSegment(offset, "api").build();
        return apiStub.getApiClient().getHttpClient().newCall(call.request().newBuilder().url(tweakedUrl).build());
    }

    private static int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
