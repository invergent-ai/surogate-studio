package net.statemesh.k8s.task.misc;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.api.PrometheusClient;
import net.statemesh.k8s.task.BaseTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.MetricType;
import net.statemesh.promql.converter.MetricData;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class PrometheusQueryTask extends BaseTask<MetricData> {
    private final MetricType metricType;
    private final PrometheusClient.MetricType prometheusMetricType;
    private final Long timeStart;
    private final Long timeEnd;
    private final Long step;
    private final String nodeId;
    private final String podName;
    private final String containerName;
    private final Integer gpuId;
    private final boolean multiValue;

    public PrometheusQueryTask(ApiStub apiStub,
                               TaskConfig config,
                               String namespace,
                               MetricType metricType,
                               PrometheusClient.MetricType prometheusMetricType,
                               String nodeId,
                               Long timeStart,
                               Long timeEnd,
                               Long step,
                               String podName,
                               String containerName,
                               Integer gpuId,
                               boolean multiValue) {
        super(apiStub, config, namespace);
        this.metricType = metricType;
        this.prometheusMetricType = prometheusMetricType;
        this.nodeId = nodeId;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.step = step;
        this.podName = podName;
        this.containerName = containerName;
        this.gpuId = gpuId;
        this.multiValue = multiValue;
    }

    @Override
    public CompletableFuture<TaskResult<MetricData>> call() {
        String scope = StringUtils.isEmpty(getNamespace()) ? "node" : "namespace";
        log.trace("Fetching Prometheus {} metrics for node {}, metric type {}",
            scope, nodeId, prometheusMetricType);
        try {
            String query = buildPrometheusQuery();
            MetricData metricsData;
            if (timeStart != null && timeEnd != null) {
                metricsData = getApiStub().getPrometheusClient().queryMetrics(
                    query,
                    timeStart,
                    timeEnd,
                    step != null ? step : 60L,
                    multiValue
                );
            } else {
                query = "last_over_time(" + query + "[10m])";
                metricsData = getApiStub().getPrometheusClient().instantQuery(query, multiValue);
            }
            if (metricsData == null) {
                return CompletableFuture.completedFuture(
                    TaskResult.<MetricData>builder()
                        .success(Boolean.FALSE)
                        .build()
                );
            }
            return CompletableFuture.completedFuture(
                TaskResult.<MetricData>builder()
                    .success(Boolean.TRUE)
                    .value(metricsData)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to fetch Prometheus metrics", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private String buildPrometheusQuery() {
        return switch (this.metricType) {
            case NODE -> PrometheusClient.NodeQueries.nodeMetrics(nodeId, prometheusMetricType);
            case CONTAINER -> PrometheusClient.NamespaceQueries.containerMetrics(
                getNamespace(),
                podName,
                containerName,
                prometheusMetricType
            );
            case GPU -> PrometheusClient.GpuQueries.gpuMetrics(
                nodeId,
                gpuId,
                prometheusMetricType
            );
            case MODEL_WORKER -> PrometheusClient.modelWorkerMetrics(
                getNamespace(),
                podName,
                prometheusMetricType
            );
            case MODEL_ROUTER -> PrometheusClient.ModelRouterQueries.modelRouterMetrics(
                getNamespace(),
                podName,
                prometheusMetricType
            );
        };
    }
}

