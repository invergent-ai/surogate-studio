package net.statemesh.service.k8s.status;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.projections.NodeStatusProjection;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiUtils;
import net.statemesh.k8s.util.MetricType;
import net.statemesh.k8s.util.Metrics;
import net.statemesh.k8s.util.ResourceStatus;
import net.statemesh.service.NodeService;
import net.statemesh.service.dto.NodeDTO;
import net.statemesh.service.dto.NodeStatsDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static net.statemesh.config.K8Timeouts.READ_METRICS_TIMEOUT_SECONDS;
import static net.statemesh.k8s.util.ApiUtils.podSummariesForNode;

@Service
public class NodeStatusService {
    private static final Logger log = LoggerFactory.getLogger(NodeStatusService.class);

    private final NodeService nodeService;
    private final KubernetesController kubernetesController;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationProperties applicationProperties;
    private final AsyncTaskExecutor smTaskExecutor;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final Map<String, Future<?>> statuses = new ConcurrentHashMap<>();

    public NodeStatusService(
        NodeService nodeService,
        KubernetesController kubernetesController,
        SimpMessagingTemplate messagingTemplate,
        ApplicationProperties applicationProperties,
        AsyncTaskExecutor smTaskExecutor,
        @Qualifier("statusScheduler") ThreadPoolTaskScheduler taskScheduler) {
        this.nodeService = nodeService;
        this.kubernetesController = kubernetesController;
        this.messagingTemplate = messagingTemplate;
        this.applicationProperties = applicationProperties;
        this.smTaskExecutor = smTaskExecutor;
        this.taskScheduler = taskScheduler;
    }

    public void startStatus(String nodeId) {
        log.debug("Starting status for node {}", nodeId);
        stopStatus(nodeId);

        final NodeDTO node = nodeService.findOne(nodeId)
            .orElseThrow(() -> new RuntimeException("Node " + nodeId + " was not found"));

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() ->
            smTaskExecutor.submit(() -> {
                TaskResult result;
                try {
                    result = this.kubernetesController.readMetrics(
                        null,
                        node.getCluster(),
                        MetricType.NODE,
                        node,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    ).get(READ_METRICS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    if (!result.isSuccess() && !Thread.currentThread().isInterrupted()) {
                        log.error("Failed to read metrics in Kubernetes for node {}", nodeId);
                        messagingTemplate.convertAndSend("/topic/stats/" + nodeId,
                            Map.of("type", "disconnect"));
                    }

                    sendStatusUpdate(
                        node.getId(),
                        readStatus(node, result,
                            podSummariesForNode(
                                kubernetesController.getApi(node.getCluster()),
                                node.getInternalName()
                            )
                        ));
                } catch (ApiException | ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException | TimeoutException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        messagingTemplate.convertAndSend("/topic/stats/" + nodeId,
                            Map.of("type", "timeout"));
                    }
                }
            }), Duration.ofSeconds(applicationProperties.getMetrics().getStatusPollInterval()));

        // Guard thread
        taskScheduler.schedule(() -> {
            if (!future.isCancelled()) {
                log.debug("Canceling status for node {} due to timeout after {} seconds",
                    nodeId, applicationProperties.getMetrics().getStatusWaitTimeout());
                future.cancel(true);
                statuses.remove(nodeId);

                messagingTemplate.convertAndSend("/topic/stats/" + nodeId,
                    Map.of(
                        "type", "timeout",
                        "error", "Connection timed out after "
                            + applicationProperties.getMetrics().getStatusWaitTimeout() + " seconds"
                    )
                );
            }
        }, Instant.now().plusSeconds(applicationProperties.getMetrics().getStatusWaitTimeout()));

        this.statuses.put(nodeId, future);
        log.debug("Status task started for node {}", nodeId);
    }

    public void stopStatus(String nodeId) {
        Future<?> status = statuses.remove(nodeId);
        if (status != null) {
            log.debug("Stopping status for node {}", nodeId);
            status.cancel(true);
        }
    }

    public NodeStatsDTO statusSnapshotForNode(String nodeId) throws ExecutionException, InterruptedException, TimeoutException {
        var node = this.nodeService.findOne(nodeId)
            .orElseThrow(() -> new RuntimeException("Node " + nodeId + " was not found"));

        return kubernetesController.readMetrics(
            null,
            node.getCluster(),
            MetricType.NODE,
            node,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ).thenApply(result -> {
            if (!result.isSuccess() && !Thread.currentThread().isInterrupted()) {
                log.error("Failed to read status in Kubernetes for node {}", node.getId());
                return null;
            }

            try {
                return readStatus(node, result,
                    podSummariesForNode(
                        kubernetesController.getApi(node.getCluster()),
                        node.getInternalName()
                    )
                );
            } catch (ApiException e) {
                log.error("Failed to build status for node {}", node.getId(), e);
                return null;
            }
        }).get(READ_METRICS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public Set<NodeStatsDTO> statusSnapshotForNodes(String clusterCid) throws ExecutionException, InterruptedException, TimeoutException {
        var futures = this.nodeService.findByClusterCid(clusterCid).stream().map(node ->
            kubernetesController.readMetrics(
                null,
                node.getCluster(),
                MetricType.NODE,
                node,
                null,
                null,
                null,
                null,
                null,
                null,
                null).thenApply(result -> {

                if (!result.isSuccess() && !Thread.currentThread().isInterrupted()) {
                    log.error("Failed to read status in Kubernetes for node {}", node.getId());
                    return null;
                }

                try {
                    return readStatus(node, result,
                        podSummariesForNode(
                            kubernetesController.getApi(node.getCluster()),
                            node.getInternalName()
                        )
                    );
                } catch (ApiException e) {
                    log.error("Failed to build status for node {}", node.getId(), e);
                    return null;
                }
            })).collect(Collectors.toSet());

        return CompletableFuture
            .allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()))
            .get(READ_METRICS_TIMEOUT_SECONDS * futures.size(), TimeUnit.SECONDS);
    }

    private NodeStatsDTO readStatus(NodeDTO node, TaskResult<Metrics> result, List<ApiUtils.AppSummary> appSummaries) {
        final Optional<NodeStatusProjection> status = nodeService.findOneAndProjectStatus(node.getId());
        return NodeStatsDTO.builder()
            .rxMbps(result.getValue() != null ? safeKB(result.getValue().getNetworkIn()) : 0)
            .txMbps(result.getValue() != null ? safeKB(result.getValue().getNetworkOut()) : 0)
            .totalApps(appSummaries.size())
            .activeApps(
                appSummaries.stream()
                    .filter(summary ->
                        ResourceStatus.ResourceStatusStage.RUNNING.equals(summary.stage())
                    ).toList().size()
            )
            .status(status.map(NodeStatusProjection::getStatus).orElse(null))
            .uptime(
                status.map(NodeStatusProjection::getLastStartTime)
                    .map(startTime -> ChronoUnit.SECONDS.between(startTime, Instant.now()))
                    .orElse(0L)
            )
            .gpuCount(result.getValue() != null ? result.getValue().getGpuCount() : 0)
            .gpuModel(result.getValue() != null ? toFixedGpuModel(result.getValue().getGpuModel()) : null)
            .gpuMemory(result.getValue() != null ? safeGpuGauge(result.getValue().getGpuMemory()) : Collections.emptyMap())
            .gpuUsage(result.getValue() != null ? safeGpuGauge(result.getValue().getGpuUsage()) : Collections.emptyMap())
            .gpuMemoryUsage(result.getValue() != null ? safeGpuGauge(result.getValue().getGpuMemoryUsage()) : Collections.emptyMap())
            .gpuMemoryFree(result.getValue() != null ? safeGpuGauge(result.getValue().getGpuMemoryFree()) : Collections.emptyMap())
            .gpuTemperature(result.getValue() != null ? safeGpuGauge(result.getValue().getGpuTemperature()) : Collections.emptyMap())
            .gpuPowerUsage(result.getValue() != null ? safeGpuGauge(result.getValue().getGpuPowerUsage()) : Collections.emptyMap())
            .build();
    }

    private void sendStatusUpdate(String nodeId, NodeStatsDTO stats) {
        log.trace("Sending {} stats to /topic/stats/{}", stats, nodeId);
        try {
            messagingTemplate.convertAndSend("/topic/stats/" + nodeId, stats);
            log.trace("Successfully sent stats to websocket for node {}", nodeId);
        } catch (Exception e) {
            log.error("Failed to send stats update to websocket for node {}", nodeId, e);
        }
    }

    private Integer safeKB(Map<Double, BigDecimal> value) {
        return safeIntValue(value) / 1024;
    }

    private Integer safeIntValue(Map<Double, BigDecimal> value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        var bytes = value.entrySet().stream().findFirst().orElse(null).getValue();
        return bytes != null ? Math.abs(bytes.intValue()) : 0;
    }

    private Map<String, Integer> safeGpuGauge(Map<String, Map<Double, BigDecimal>> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }

        // We want the first gauge value for each GPU
        return value.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> safeIntValue(entry.getValue())
            ));
    }

    private String toFixedGpuModel(String providedGpuModel) {
        if (StringUtils.contains(providedGpuModel, "5090")) {
            return "rtx5090";
        } else if (StringUtils.contains(providedGpuModel, "6000")) {
            return "rtx6000pro";
        } else
            return "rtx4070ti"; // for development, assume 4070ti
    }
}
