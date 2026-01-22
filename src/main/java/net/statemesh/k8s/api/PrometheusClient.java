package net.statemesh.k8s.api;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.promql.builder.InstantQueryBuilder;
import net.statemesh.promql.builder.QueryBuilderType;
import net.statemesh.promql.builder.RangeQueryBuilder;
import net.statemesh.promql.converter.ConvertUtil;
import net.statemesh.promql.converter.MetricData;
import net.statemesh.promql.converter.query.DefaultQueryResult;
import net.statemesh.promql.converter.query.ListVectorData;
import net.statemesh.promql.converter.query.MatrixData;
import net.statemesh.promql.converter.query.VectorData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
public class PrometheusClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PrometheusClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public MatrixData queryMetrics(
        String query,
        long timeStartEpochSeconds,
        long timeEndEpochSeconds,
        long step,
        boolean multiValue) {
        if (StringUtils.isEmpty(query)) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        log.trace("Querying Prometheus with query: {}", query);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");

            RangeQueryBuilder rangeQueryBuilder = QueryBuilderType.RangeQuery.newInstance(baseUrl);
            URI targetUri = rangeQueryBuilder.withQuery(query)
                .withStartEpochTime(timeStartEpochSeconds)
                .withEndEpochTime(timeEndEpochSeconds)
                .withStepTime(step + "s")
                .build();

            ResponseEntity<String> response = restTemplate.exchange(
                targetUri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from " + baseUrl);
            }

            DefaultQueryResult<MatrixData> converted = ConvertUtil.convertQueryResultString(response.getBody());
            return converted.getResult().isEmpty() ? new MatrixData() : converted.getResult().getFirst();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Prometheus data from " + baseUrl, e);
        }
    }

    public MetricData instantQuery(String query, boolean multiValue) {
        if (StringUtils.isEmpty(query)) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");

            InstantQueryBuilder iqb = QueryBuilderType.InstantQuery.newInstance(baseUrl);
            URI uri = iqb.withQuery(query)
                .build();

            ResponseEntity<String> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from " + baseUrl);
            }

            DefaultQueryResult<VectorData> converted = ConvertUtil.convertQueryResultString(response.getBody());
            if (converted.getResult().isEmpty()) {
                return new VectorData();
            }

            if (multiValue) {
                return new ListVectorData(converted.getResult());
            }
            return converted.getResult().getFirst();
        } catch (Exception e) {
            log.error("Failed to execute query: {}", query);
            throw new RuntimeException("Failed to fetch Prometheus data from " + baseUrl, e);
        }
    }

    public boolean isConfigured() {
        return !StringUtils.isEmpty(baseUrl);
    }

    public static class NodeQueries {
        // Specific node metrics query builders
        public static String nodeMetrics(String nodeName, MetricType type) {
            String baseQuery = switch (type) {
                case CPU ->
                    "sum(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{node=~\"%s\"})";
                case MEMORY ->
                    "sum(node_namespace_pod_container:container_memory_working_set_bytes{node=~\"%s\", container!=\"\"})";
                case NETWORK_IN -> "sum(rate(container_network_receive_bytes_total{node=~\"%s\"}[10m]))";
                case NETWORK_OUT -> "sum(rate(container_network_transmit_bytes_total{node=~\"%s\"}[10m]))";
                case GPU_COUNT -> "DCGM_FI_DEV_COUNT{Hostname=~\"%s\"}";
                case GPU_TOTAL_MEM -> "DCGM_FI_DEV_FB_TOTAL{Hostname=~\"%s\"}";
                case GPU_MEM_USAGE -> "DCGM_FI_DEV_FB_USED{Hostname=~\"%s\"}";
                case GPU_MEM_FREE -> "DCGM_FI_DEV_FB_FREE{Hostname=~\"%s\"}";
                default -> throw new IllegalArgumentException("Unsupported Node metric type: " + type);

            };
            return String.format(baseQuery, nodeName);
        }
    }

    public static class NamespaceQueries {
        // Specific container metrics query builders
        public static String containerMetrics(String namespace, String podName, String containerName, MetricType type) {
            return switch (type) {
                case CPU -> String.format(
                    "container_cpu_usage_seconds_total{namespace=\"%s\",pod=\"%s\",container=\"%s\"}",
                    namespace, podName, containerName
                );
                case MEMORY -> String.format(
                    "container_memory_working_set_bytes{namespace=\"%s\",pod=\"%s\",container=\"%s\"}",
                    namespace, podName, containerName
                );
                case NETWORK_IN -> String.format(
                    "container_network_receive_bytes_total{namespace=\"%s\",pod=\"%s\"}",
                    namespace, podName
                );
                case NETWORK_OUT -> String.format(
                    "container_network_transmit_bytes_total{namespace=\"%s\",pod=\"%s\"}",
                    namespace, podName
                );
                default -> throw new IllegalArgumentException("Unsupported Container metric type: " + type);
            };
        }
    }

    public static class GpuQueries {
        public static String gpuMetrics(String nodeName, Integer gpuId, MetricType type) {
            String baseQuery = switch (type) {
                case GPU_USAGE -> "DCGM_FI_DEV_GPU_UTIL{Hostname=~\"%s\", gpu=~\"%d\"}";
                case GPU_TOTAL_MEM -> "DCGM_FI_DEV_FB_TOTAL{Hostname=~\"%s\", gpu=~\"%d\"}";
                case GPU_MEM_USAGE -> "DCGM_FI_DEV_FB_USED{Hostname=~\"%s\", gpu=~\"%d\"}";
                case GPU_MEM_FREE -> "DCGM_FI_DEV_FB_FREE{Hostname=~\"%s\", gpu=~\"%d\"}";
                case GPU_TEMPERATURE -> "DCGM_FI_DEV_GPU_TEMP{Hostname=~\"%s\", gpu=~\"%d\"}";
                case GPU_POWER_USAGE -> "DCGM_FI_DEV_POWER_USAGE{Hostname=~\"%s\", gpu=~\"%d\"}";
                default -> throw new IllegalArgumentException("Invalid metric type for GPU metrics: " + type);

            };
            return String.format(baseQuery, nodeName, gpuId);
        }
    }

    public static String modelWorkerMetrics(String namespace, String podName, MetricType type) {
        return switch (type) {
            // Core inference metrics - using actual vLLM metric names
            case MODEL_REQUESTS_RUNNING ->
                "vllm:num_requests_running{engine=\"0\"}";
            case MODEL_REQUESTS_WAITING ->
                "vllm:num_requests_waiting{engine=\"0\"}";
            case MODEL_SWAPPED_REQUESTS ->
                "vllm:num_requests_swapped{engine=\"0\"}";

            // Cache metrics - using correct vLLM names
            case MODEL_KV_CACHE_USAGE ->
                "vllm:kv_cache_usage_perc{engine=\"0\"}";
            case MODEL_CACHE_HIT_RATE ->
                "vllm:gpu_prefix_cache_hit_rate{engine=\"0\"}";
            case MODEL_CACHE_HITS_TOTAL ->
                "vllm:gpu_prefix_cache_hits_total{engine=\"0\"}";
            case MODEL_CACHE_QUERIES_TOTAL ->
                "vllm:gpu_prefix_cache_queries_total{engine=\"0\"}";

            // Performance metrics - calculated from vLLM histograms
            case MODEL_PROMPT_TOKENS_RATE ->
                "rate(vllm:prompt_tokens_total{engine=\"0\"}[5m])";
            case MODEL_GENERATION_TOKENS_RATE ->
                "rate(vllm:generation_tokens_total{engine=\"0\"}[5m])";

            // Timing metrics - calculated from histogram sums and counts
            case MODEL_TIME_TO_FIRST_TOKEN ->
                "vllm:time_to_first_token_seconds_sum{engine=\"0\"} / vllm:time_to_first_token_seconds_count{engine=\"0\"}";
            case MODEL_TIME_PER_OUTPUT_TOKEN ->
                "vllm:time_per_output_token_seconds_sum{engine=\"0\"} / vllm:time_per_output_token_seconds_count{engine=\"0\"}";
            case MODEL_PREFILL_TIME ->
                "vllm:request_prefill_time_seconds_sum{engine=\"0\"} / vllm:request_prefill_time_seconds_count{engine=\"0\"}";
            case MODEL_DECODE_TIME ->
                "vllm:request_decode_time_seconds_sum{engine=\"0\"} / vllm:request_decode_time_seconds_count{engine=\"0\"}";

            // Health metrics
            case MODEL_HEALTHY_PODS ->
                "up{job=\"vllm\"}";
            case MODEL_CURRENT_QPS ->
                "rate(vllm:request_success_total{engine=\"0\"}[5m])";

            default -> throw new IllegalArgumentException("Invalid metric type for Model Worker: " + type);
        };
    }

    public static class ModelRouterQueries {
        public static String modelRouterMetrics(String namespace, String podName, MetricType type) {
            return switch (type) {
                // Router performance metrics - use HTTP metrics since router handles HTTP requests
                case ROUTER_REQUESTS_TOTAL ->
                    "rate(http_requests_total{handler=\"/v1/completions\", method=\"POST\"}[5m])";

                case ROUTER_REQUESTS_DURATION ->
                    "http_request_duration_highr_seconds_sum / http_request_duration_highr_seconds_count";

                case ROUTER_ACTIVE_CONNECTIONS ->
                    "sum by (server) (vllm:num_requests_running{server=~\".*\"}) + on(server) sum by (server) (vllm:num_requests_waiting{server=~\".*\"} or vector(0))";

                // Health and load using vLLM metrics from downstream workers
                case ROUTER_WORKER_HEALTH ->
                    "vllm:healthy_pods_total{server=~\".*\"}";

                case ROUTER_WORKER_LOAD ->
                    "vllm:current_qps{server=~\".*\"}";

                case ROUTER_ROUTING_DECISIONS ->
                    "rate(http_requests_total[5m])";

                case ROUTER_CPU_USAGE ->
                    "router_cpu_usage_percent";
                case ROUTER_MEMORY_USAGE ->
                    "router_memory_usage_percent";
                case ROUTER_DISK_USAGE ->
                    "router_disk_usage_percent";

                // Error rates - calculate from HTTP metrics
                case ROUTER_ERROR_RATE ->
                    "rate(http_requests_total{status=~\"4xx|5xx\"}[5m]) / rate(http_requests_total[5m]) * 100";
                case ROUTER_TIMEOUT_RATE ->
                    "rate(http_request_duration_seconds{le=\"+Inf\"}[5m]) - rate(http_request_duration_seconds{le=\"30\"}[5m])";

                default -> throw new IllegalArgumentException("Invalid metric type for Model Router: " + type);
            };
        }
    }

    public enum MetricType {
        CPU,
        MEMORY,
        NETWORK_IN,
        NETWORK_OUT,
        GPU_COUNT,
        GPU_USAGE,
        GPU_TOTAL_MEM,
        GPU_MEM_USAGE,
        GPU_MEM_FREE,
        GPU_TEMPERATURE,
        GPU_POWER_USAGE,

        // Model Worker metrics
        MODEL_REQUESTS_RUNNING,
        MODEL_REQUESTS_WAITING,
        MODEL_KV_CACHE_USAGE,
        MODEL_PROMPT_TOKENS_RATE,
        MODEL_GENERATION_TOKENS_RATE,
        MODEL_REQUEST_SUCCESS_TOTAL,
        MODEL_TIME_TO_FIRST_TOKEN,
        MODEL_TIME_PER_OUTPUT_TOKEN,
        MODEL_E2E_LATENCY,
        MODEL_QUEUE_TIME,
        MODEL_INFERENCE_TIME,
        MODEL_PREFILL_TIME,
        MODEL_DECODE_TIME,
        MODEL_SWAPPED_REQUESTS,
        MODEL_CACHE_HIT_RATE,
        MODEL_CACHE_HITS_TOTAL,
        MODEL_CACHE_QUERIES_TOTAL,
        MODEL_HEALTHY_PODS,
        MODEL_CURRENT_QPS,

        // Model Router metrics
        ROUTER_REQUESTS_TOTAL,
        ROUTER_REQUESTS_DURATION,
        ROUTER_ACTIVE_CONNECTIONS,
        ROUTER_WORKER_HEALTH,
        ROUTER_WORKER_LOAD,
        ROUTER_ROUTING_DECISIONS,
        ROUTER_ERROR_RATE,
        ROUTER_TIMEOUT_RATE,
        ROUTER_CPU_USAGE,
        ROUTER_MEMORY_USAGE,
        ROUTER_DISK_USAGE
    }
}
