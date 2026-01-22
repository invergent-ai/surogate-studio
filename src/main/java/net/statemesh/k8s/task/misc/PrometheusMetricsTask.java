package net.statemesh.k8s.task.misc;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.api.PrometheusClient;
import net.statemesh.k8s.task.BaseTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.MetricType;
import net.statemesh.k8s.util.Metrics;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.promql.converter.MetricData;
import net.statemesh.promql.converter.query.ListVectorData;
import net.statemesh.promql.converter.query.MatrixData;
import net.statemesh.promql.converter.query.VectorData;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.ContainerDTO;
import net.statemesh.service.dto.NodeDTO;
import net.statemesh.service.util.ProfileUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.statemesh.k8s.util.ApiUtils.podName;

@Slf4j
public class PrometheusMetricsTask extends BaseTask<Metrics> {
    private final MetricType metricType;
    private final NodeDTO node;
    private final ApplicationDTO application;
    private final ContainerDTO container;
    private final Integer gpuId;
    private final String podName;

    private final Long timeStart;
    private final Long timeEnd;
    private final Long step;

    public PrometheusMetricsTask(ApiStub apiStub,
                                 TaskConfig taskConfig,
                                 String namespace,
                                 MetricType metricType,
                                 NodeDTO node,
                                 ApplicationDTO application,
                                 ContainerDTO container,
                                 Integer gpuId,
                                 String podName,
                                 Long timeStart,
                                 Long timeEnd,
                                 Long step) {
        super(apiStub, taskConfig, namespace);
        this.metricType = metricType;
        this.node = node;
        this.application = application;
        this.container = container;
        this.gpuId = gpuId;
        this.podName = podName;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.step = step;
    }

    @Override
    public CompletableFuture<TaskResult<Metrics>> call() {
        log.trace("Get metrics of type {}", metricType);
        try {
            String resolvedPodName = null;
            String resolvedContainerName = null;

            if (application != null) {
                if (podName != null) {
                    resolvedPodName = podName;
                } else {
                    resolvedPodName = podName(
                        getApiStub().getCoreV1Api(),
                        getNamespace(),
                        application.getInternalName()
                    );
                }

                if (container != null) {
                    resolvedContainerName = NamingUtils.containerName(
                        application.getInternalName(),
                        container.getImageName()
                    );
                }
            }

            return switch (metricType) {
                case NODE -> getNodeMetrics(node);
                case CONTAINER -> getContainerMetrics(resolvedPodName, resolvedContainerName);
                case GPU -> getGpuMetrics(gpuId);
                case MODEL_WORKER -> getModelWorkerMetrics(resolvedPodName);
                case MODEL_ROUTER -> getModelRouterMetrics(resolvedPodName);
            };
        } catch (Exception e) {
            log.error("Error getting metrics", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<TaskResult<Metrics>> getNodeMetrics(NodeDTO node) {
        var networkInMetrics = queryPrometheusMetrics(
            MetricType.NODE,
            PrometheusClient.MetricType.NETWORK_IN,
            node != null ? node.getInternalName() : null,
            null,
            null,
            null,
            null,
            null,
            null,
            false
        );

        var networkOutMetrics = queryPrometheusMetrics(
            MetricType.NODE,
            PrometheusClient.MetricType.NETWORK_OUT,
            node != null ? node.getInternalName() : null,
            null,
            null,
            null,
            null,
            null,
            null,
            false
        );

        var gpuCountMetrics = CompletableFuture.<TaskResult<MetricData>>completedFuture(null);
        var gpuTotalMemoryMetrics = CompletableFuture.<TaskResult<MetricData>>completedFuture(null);
        var gpuMemoryUsageMetrics = CompletableFuture.<TaskResult<MetricData>>completedFuture(null);
        var gpuMemoryFreeMetrics = CompletableFuture.<TaskResult<MetricData>>completedFuture(null);

        if (ProfileUtil.isAppliance(getApiStub().getEnvironment())) {
            gpuCountMetrics = queryPrometheusMetrics(
                MetricType.NODE,
                PrometheusClient.MetricType.GPU_COUNT,
                node != null ? node.getInternalName() : null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            );
            gpuTotalMemoryMetrics = queryPrometheusMetrics(
                MetricType.NODE,
                PrometheusClient.MetricType.GPU_TOTAL_MEM,
                node != null ? node.getInternalName() : null,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            );
            gpuMemoryUsageMetrics = queryPrometheusMetrics(
                MetricType.NODE,
                PrometheusClient.MetricType.GPU_MEM_USAGE,
                node != null ? node.getInternalName() : null,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            );
            gpuMemoryFreeMetrics = queryPrometheusMetrics(
                MetricType.NODE,
                PrometheusClient.MetricType.GPU_MEM_FREE,
                node != null ? node.getInternalName() : null,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            );
        }

        CompletableFuture<TaskResult<MetricData>> finalGpuCountMetrics = gpuCountMetrics;
        CompletableFuture<TaskResult<MetricData>> finalGpuTotalMemoryMetricsMetrics = gpuTotalMemoryMetrics;
        CompletableFuture<TaskResult<MetricData>> finalGpuMemoryUsageMetrics = gpuMemoryUsageMetrics;
        CompletableFuture<TaskResult<MetricData>> finalGpuMemoryFreeMetrics = gpuMemoryFreeMetrics;

        return CompletableFuture.allOf(
            networkInMetrics,
            networkOutMetrics,
            gpuCountMetrics,
            gpuTotalMemoryMetrics,
            gpuMemoryUsageMetrics,
            gpuMemoryFreeMetrics
        ).thenApply(v -> {
            try {
                var networkInResult = networkInMetrics.get();
                var networkOutResult = networkOutMetrics.get();
                var gpuCountResult = finalGpuCountMetrics.get();
                var gpuTotalMemoryResult = finalGpuTotalMemoryMetricsMetrics.get();
                var gpuMemoryUsageResult = finalGpuMemoryUsageMetrics.get();
                var gpuMemoryFreeResult = finalGpuMemoryFreeMetrics.get();

                if (!networkInResult.isSuccess() || !networkOutResult.isSuccess()) {
                    return TaskResult.fail();
                }

                return TaskResult.<Metrics>builder()
                    .success(true)
                    .value(Metrics.builder()
                        .networkIn(parseMetricData(networkInResult))
                        .networkOut(parseMetricData(networkOutResult))
                        .gpuCount(parseGpuCount(gpuCountResult))
                        .gpuModel(parseGpuModel(gpuCountResult))
                        .gpuMemory(parseGpuMetricData(gpuTotalMemoryResult))
                        .gpuMemoryUsage(parseGpuMetricData(gpuMemoryUsageResult))
                        .gpuMemoryFree(parseGpuMetricData(gpuMemoryFreeResult))
                        .created(Instant.now())
                        .build())
                    .build();
            } catch (Exception e) {
                log.error("Error combining metrics", e);
                return TaskResult.fail();
            }
        });
    }

    private CompletableFuture<TaskResult<Metrics>> getGpuMetrics(Integer gpuId) {
        var gpuUsageMetrics = queryPrometheusMetrics(
            MetricType.GPU,
            PrometheusClient.MetricType.GPU_USAGE,
            node != null ? node.getInternalName() : null,
            null,
            null,
            gpuId,
            timeStart,
            timeEnd,
            step,
            false
        );
        var gpuTotalMemoryMetrics = queryPrometheusMetrics(
            MetricType.GPU,
            PrometheusClient.MetricType.GPU_TOTAL_MEM,
            node != null ? node.getInternalName() : null,
            null,
            null,
            gpuId,
            timeStart,
            timeEnd,
            step,
            false
        );
        var gpuMemoryUsageMetrics = queryPrometheusMetrics(
            MetricType.GPU,
            PrometheusClient.MetricType.GPU_MEM_USAGE,
            node != null ? node.getInternalName() : null,
            null,
            null,
            gpuId,
            timeStart,
            timeEnd,
            step,
            false
        );
        var gpuMemoryFreeMetrics = queryPrometheusMetrics(
            MetricType.GPU,
            PrometheusClient.MetricType.GPU_MEM_FREE,
            node != null ? node.getInternalName() : null,
            null,
            null,
            gpuId,
            timeStart,
            timeEnd,
            step,
            false
        );
        var gpuTemperatureMetrics = queryPrometheusMetrics(
            MetricType.GPU,
            PrometheusClient.MetricType.GPU_TEMPERATURE,
            node != null ? node.getInternalName() : null,
            null,
            null,
            gpuId,
            timeStart,
            timeEnd,
            step,
            false
        );
        var gpuPowerUsageMetrics = queryPrometheusMetrics(
            MetricType.GPU,
            PrometheusClient.MetricType.GPU_POWER_USAGE,
            node != null ? node.getInternalName() : null,
            null,
            null,
            gpuId,
            timeStart,
            timeEnd,
            step,
            false
        );

        return CompletableFuture.allOf(
            gpuUsageMetrics,
            gpuTotalMemoryMetrics,
            gpuMemoryUsageMetrics,
            gpuMemoryFreeMetrics,
            gpuTemperatureMetrics,
            gpuPowerUsageMetrics
        ).thenApply(v -> {
            try {
                var gpuUsageResult = gpuUsageMetrics.get();
                var gpuTotalMemoryResult = gpuTotalMemoryMetrics.get();
                var gpuMemoryUsageResult = gpuMemoryUsageMetrics.get();
                var gpuMemoryFreeResult = gpuMemoryFreeMetrics.get();
                var gpuTemperatureResult = gpuTemperatureMetrics.get();
                var gpuPowerUsageResult = gpuPowerUsageMetrics.get();

                if (!gpuUsageResult.isSuccess() ||
                    !gpuTotalMemoryResult.isSuccess() || !gpuMemoryUsageResult.isSuccess() ||
                    !gpuTemperatureResult.isSuccess() || !gpuPowerUsageResult.isSuccess()
                ) {
                    return TaskResult.fail();
                }

                return TaskResult.<Metrics>builder()
                    .success(true)
                    .value(Metrics.builder()
                        .gpuUsage(parseGpuMetricData(gpuUsageResult))
                        .gpuMemory(parseGpuMetricData(gpuTotalMemoryResult))
                        .gpuMemoryUsage(parseGpuMetricData(gpuMemoryUsageResult))
                        .gpuMemoryFree(parseGpuMetricData(gpuMemoryFreeResult))
                        .gpuTemperature(parseGpuMetricData(gpuTemperatureResult))
                        .gpuPowerUsage(parseGpuMetricData(gpuPowerUsageResult))
                        .created(Instant.now())
                        .build())
                    .build();
            } catch (Exception e) {
                log.error("Error combining metrics", e);
                return TaskResult.fail();
            }
        });
    }

    private CompletableFuture<TaskResult<Metrics>> getContainerMetrics(String podName, String containerName) {
        var cpuMetrics = queryPrometheusMetrics(
            MetricType.CONTAINER,
            PrometheusClient.MetricType.CPU,
            null,
            podName,
            containerName,
            null,
            timeStart,
            timeEnd,
            step,
            false
        );

        var memoryMetrics = queryPrometheusMetrics(
            MetricType.CONTAINER,
            PrometheusClient.MetricType.MEMORY,
            null,
            podName,
            containerName,
            null,
            timeStart,
            timeEnd,
            step,
            false
        );

        var networkInMetrics = queryPrometheusMetrics(
            MetricType.CONTAINER,
            PrometheusClient.MetricType.NETWORK_IN,
            null,
            podName,
            containerName,
            null,
            timeStart,
            timeEnd,
            step,
            false
        );

        var networkOutMetrics = queryPrometheusMetrics(
            MetricType.CONTAINER,
            PrometheusClient.MetricType.NETWORK_OUT,
            null,
            podName,
            containerName,
            null,
            timeStart,
            timeEnd,
            step,
            false
        );

        return CompletableFuture.allOf(
            cpuMetrics,
            memoryMetrics,
            networkInMetrics,
            networkOutMetrics
        ).thenApply(v -> {
            try {
                var cpuResult = cpuMetrics.get();
                var memoryResult = memoryMetrics.get();
                var networkInResult = networkInMetrics.get();
                var networkOutResult = networkOutMetrics.get();

                if (!cpuResult.isSuccess() || !memoryResult.isSuccess() ||
                    !networkInResult.isSuccess() ||
                    !networkOutResult.isSuccess()) {
                    return TaskResult.fail();
                }

                return TaskResult.<Metrics>builder()
                    .success(true)
                    .value(Metrics.builder()
                        .cpu(parseMetricData(cpuResult))
                        .memory(parseMetricData(memoryResult))
                        .networkIn(parseMetricData(networkInResult))
                        .networkOut(parseMetricData(networkOutResult))
                        .created(Instant.now())
                        .build())
                    .build();
            } catch (Exception e) {
                log.error("Error combining metrics", e);
                return TaskResult.fail();
            }
        });
    }
    private CompletableFuture<TaskResult<Metrics>> getModelWorkerMetrics(String podName) {
        // Core inference metrics
        var requestsRunningMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_REQUESTS_RUNNING,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var requestsWaitingMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_REQUESTS_WAITING,
            null, podName, null, null, timeStart, timeEnd, step, false
        );


        var kvCacheUsageMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_KV_CACHE_USAGE,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var cacheHitRateMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_CACHE_HIT_RATE,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var cacheHitsMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_CACHE_HITS_TOTAL,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var cacheQueriesMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_CACHE_QUERIES_TOTAL,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var healthyPodsMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_HEALTHY_PODS,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var currentQpsMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_CURRENT_QPS,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var promptTokensRateMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_PROMPT_TOKENS_RATE,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var generationTokensRateMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_GENERATION_TOKENS_RATE,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var timeToFirstTokenMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_TIME_TO_FIRST_TOKEN,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var timePerOutputTokenMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_TIME_PER_OUTPUT_TOKEN,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var prefillTimeMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_PREFILL_TIME,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var decodeTimeMetrics = queryPrometheusMetrics(
            MetricType.MODEL_WORKER,
            PrometheusClient.MetricType.MODEL_DECODE_TIME,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        return CompletableFuture.allOf(
            requestsRunningMetrics,
            requestsWaitingMetrics,
            kvCacheUsageMetrics,
            cacheHitRateMetrics,
            cacheHitsMetrics,
            cacheQueriesMetrics,
            healthyPodsMetrics,
            currentQpsMetrics,
            promptTokensRateMetrics,
            generationTokensRateMetrics,
            timeToFirstTokenMetrics,
            timePerOutputTokenMetrics,
            prefillTimeMetrics,
            decodeTimeMetrics
        ).thenApply(v -> {
            try {
                var requestsRunningResult = requestsRunningMetrics.get();
                var requestsWaitingResult = requestsWaitingMetrics.get();
                var kvCacheUsageResult = kvCacheUsageMetrics.get();
                var cacheHitRateResult = cacheHitRateMetrics.get();
                var cacheHitsResult = cacheHitsMetrics.get();
                var cacheQueriesResult = cacheQueriesMetrics.get();
                var healthyPodsResult = healthyPodsMetrics.get();
                var currentQpsResult = currentQpsMetrics.get();
                var promptTokensRateResult = promptTokensRateMetrics.get();
                var generationTokensRateResult = generationTokensRateMetrics.get();
                var timeToFirstTokenResult = timeToFirstTokenMetrics.get();
                var timePerOutputTokenResult = timePerOutputTokenMetrics.get();
                var prefillTimeResult = prefillTimeMetrics.get();
                var decodeTimeResult = decodeTimeMetrics.get();

                // Build metrics with the parsed data
                return TaskResult.<Metrics>builder()
                    .success(true)
                    .value(Metrics.builder()
                        .modelRequestsRunning(parseMetricData(requestsRunningResult))
                        .modelRequestsWaiting(parseMetricData(requestsWaitingResult))
                        .modelKvCacheUsage(parseMetricData(kvCacheUsageResult))
                        .modelCacheHitRate(parseMetricData(cacheHitRateResult))
                        .modelCacheHitsTotal(parseMetricData(cacheHitsResult))
                        .modelCacheQueriesTotal(parseMetricData(cacheQueriesResult))
                        .modelHealthyPods(parseMetricData(healthyPodsResult))
                        .modelCurrentQps(parseMetricData(currentQpsResult))
                        .modelPromptTokensRate(parseMetricData(promptTokensRateResult))
                        .modelGenerationTokensRate(parseMetricData(generationTokensRateResult))
                        .modelTimeToFirstToken(parseMetricData(timeToFirstTokenResult))
                        .modelTimePerOutputToken(parseMetricData(timePerOutputTokenResult))
                        .modelPrefillTime(parseMetricData(prefillTimeResult))
                        .modelDecodeTime(parseMetricData(decodeTimeResult))
                        .created(Instant.now())
                        .build())
                    .build();
            } catch (Exception e) {
                log.error("Error combining model worker metrics", e);
                return TaskResult.fail();
            }
        });
    }

    private CompletableFuture<TaskResult<Metrics>> getModelRouterMetrics(String podName) {
        // Routing metrics
        var requestsTotalMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_REQUESTS_TOTAL,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var requestsDurationMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_REQUESTS_DURATION,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var activeConnectionsMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_ACTIVE_CONNECTIONS,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var workerHealthMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_WORKER_HEALTH,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var workerLoadMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_WORKER_LOAD,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var routingDecisionsMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_ROUTING_DECISIONS,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var cpuUsageMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_CPU_USAGE,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var memoryUsageMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_MEMORY_USAGE,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var diskUsageMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_DISK_USAGE,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var errorRateMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_ERROR_RATE,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        var timeoutRateMetrics = queryPrometheusMetrics(
            MetricType.MODEL_ROUTER,
            PrometheusClient.MetricType.ROUTER_TIMEOUT_RATE,
            null, podName, null, null, timeStart, timeEnd, step, false
        );

        return CompletableFuture.allOf(
            requestsTotalMetrics,
            requestsDurationMetrics,
            activeConnectionsMetrics,
            workerHealthMetrics,
            workerLoadMetrics,
            routingDecisionsMetrics,
            cpuUsageMetrics,
            memoryUsageMetrics,
            diskUsageMetrics,
            errorRateMetrics,
            timeoutRateMetrics
        ).thenApply(v -> {
            try {
                var requestsTotalResult = requestsTotalMetrics.get();
                var requestsDurationResult = requestsDurationMetrics.get();
                var activeConnectionsResult = activeConnectionsMetrics.get();
                var workerHealthResult = workerHealthMetrics.get();
                var workerLoadResult = workerLoadMetrics.get();
                var routingDecisionsResult = routingDecisionsMetrics.get();
                var cpuUsageResult = cpuUsageMetrics.get();
                var memoryUsageResult = memoryUsageMetrics.get();
                var diskUsageResult = diskUsageMetrics.get();
                var errorRateResult = errorRateMetrics.get();
                var timeoutRateResult = timeoutRateMetrics.get();

                // Build metrics with the parsed data
                return TaskResult.<Metrics>builder()
                    .success(true)
                    .value(Metrics.builder()
                        .routerRequestsTotal(parseMetricData(requestsTotalResult))
                        .routerRequestsDuration(parseMetricData(requestsDurationResult))
                        .routerActiveConnections(parseMetricData(activeConnectionsResult))
                        .routerWorkerHealth(parseMetricData(workerHealthResult))
                        .routerWorkerLoad(parseMetricData(workerLoadResult))
                        .routerRoutingDecisions(parseMetricData(routingDecisionsResult))
                        .routerCpuUsage(parseMetricData(cpuUsageResult))
                        .routerMemoryUsage(parseMetricData(memoryUsageResult))
                        .routerDiskUsage(parseMetricData(diskUsageResult))
                        .routerErrorRate(parseMetricData(errorRateResult))
                        .routerTimeoutRate(parseMetricData(timeoutRateResult))
                        .created(Instant.now())
                        .build())
                    .build();
            } catch (Exception e) {
                log.error("Error combining model router metrics", e);
                return TaskResult.fail();
            }
        });
    }

    private CompletableFuture<TaskResult<MetricData>> queryPrometheusMetrics(
        MetricType metricType,
        PrometheusClient.MetricType prometheusMetricType,
        String nodeName,
        String podName,
        String containerName,
        Integer gpuId,
        Long timeStart,
        Long timeEnd,
        Long step,
        boolean multiValue
    ) {
        PrometheusQueryTask queryTask = new PrometheusQueryTask(
            getApiStub(),
            getTaskConfig(),
            getNamespace(),
            metricType,
            prometheusMetricType,
            nodeName,
            timeStart,
            timeEnd,
            step,
            podName,
            containerName,
            gpuId,
            multiValue
        );
        // execute  asynchronously so multiple metrics can be fetched in parallel
        return CompletableFuture.supplyAsync(() -> {
            try {
                return queryTask.call().join();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String parseGpuModel(TaskResult<MetricData> taskResult) {
        if (taskResult == null) {
            return null;
        }
        if (taskResult.getValue() instanceof VectorData data) {
            return data.getMetric().get("DCGM_FI_DEV_NAME");
        } else if (taskResult.getValue() instanceof MatrixData data) {
            return data.getMetric().get("DCGM_FI_DEV_NAME");
        } else {
            log.warn("Invalid gpu model metric: {}", taskResult.getValue());
            return "unknown";
        }
    }

    private Integer parseGpuCount(TaskResult<MetricData> taskResult) {
        if (taskResult == null) {
            return 0;
        }

        if (taskResult.getValue() instanceof VectorData data) {
            return (int) data.getValue();
        } else if (taskResult.getValue() instanceof MatrixData data) {
            return (int) data.getValues()[0];
        } else {
            log.warn("Invalid gpu count metric: {}", taskResult.getValue());
            return 0;
        }
    }

    private Map<String, Map<Double, BigDecimal>> parseGpuMetricData(TaskResult<MetricData> taskResult) {
        Map<String, Map<Double, BigDecimal>> result = new HashMap<>();
        if (taskResult == null) {
            return result;
        }

        if (taskResult.getValue() instanceof ListVectorData value) {
            result = value.stream().collect(Collectors.toMap(
                entry -> entry.getMetric().get("gpu"),
                entry -> Map.of(entry.getTimestamp(), BigDecimal.valueOf(entry.getValue())),
                (left, right) -> left
            ));
        } else {
            if (gpuId != null) {
                result.put(gpuId.toString(), parseMetricData(taskResult));
            }
        }

        return result;

    }

    private Map<Double, BigDecimal> parseMetricData(TaskResult<MetricData> taskResult) {
        Map<Double, BigDecimal> result = new HashMap<>();
        if (taskResult == null) {
            return result;
        }

        if (taskResult.getValue() instanceof VectorData data) {
            result.put(safeTimestamp(data.getTimestamp()), BigDecimal.valueOf(data.getValue()));
        } else if (taskResult.getValue() instanceof MatrixData data) {
            for (int i = 0; i < data.getTimestamps().length; i++) {
                result.put(safeTimestamp(data.getTimestamps()[i]), BigDecimal.valueOf(data.getValues()[i]));
            }
        }
        return result;
    }

    private double safeTimestamp(double timestamp) {
        return timestamp == 0 ? Instant.now().getEpochSecond() : timestamp;
    }
}
