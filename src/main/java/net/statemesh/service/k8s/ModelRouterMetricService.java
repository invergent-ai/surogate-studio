package net.statemesh.service.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.flow.CreateModelFlow;
import net.statemesh.k8s.util.MetricType;
import net.statemesh.k8s.util.Metrics;
import net.statemesh.service.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.statemesh.config.K8Timeouts.READ_METRICS_TIMEOUT_SECONDS;

@Service
@Slf4j
public class ModelRouterMetricService extends PollingEventStreamService {
    private final ObjectMapper objectMapper;

    public ModelRouterMetricService(
        ApplicationService applicationService,
        ContainerService containerService,
        TaskRunService taskRunService,
        RayJobService rayJobService,
        KubernetesController kubernetesController,
        ApplicationProperties applicationProperties,
        @Qualifier("statusScheduler") ThreadPoolTaskScheduler taskScheduler,
        ObjectMapper objectMapper) {

        super(applicationService, containerService, taskRunService, rayJobService, kubernetesController, applicationProperties, taskScheduler);

        this.objectMapper = objectMapper;
    }

    @Override
    protected void onTick(String... emId) throws InterruptedException, TimeoutException {
        final var applicationId = emId[0];
        final var podName = emId[1];
        final var containerName = emId[2];

        try {
            var application = applicationService.findOne(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application " + applicationId + " does not exist"));

            var routerApp = CreateModelFlow.scaffoldRouterApplication(application, objectMapper);

            var result = kubernetesController.readMetrics(
                application.getProject().getNamespace(),
                application.getProject().getCluster(),
                MetricType.MODEL_ROUTER,
                null,
                routerApp,
                null,
                null,
                podName,
                Instant.now().atZone(ZoneOffset.UTC).minusMinutes(60).toEpochSecond() * 1000,
                Instant.now().atZone(ZoneOffset.UTC).toEpochSecond() * 1000,
                pollInterval
            ).get(READ_METRICS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.isSuccess() && !Thread.currentThread().isInterrupted()) {
                log.warn("Failed to read model router metrics for application {} container {}", applicationId, containerName);
                return;
            }

            var flattenedMetrics = flattenRouterMetrics(result.getValue());
            sendEvent("metrics", flattenedMetrics, emId);

        } catch (ExecutionException e) {
            log.error("Error reading model router metrics for application {}: {}", applicationId, e.getMessage());
        }
    }

    private Map<String, Object> flattenRouterMetrics(Metrics metrics) {
        Map<String, Object> flattened = new HashMap<>();

        try {
            double requestsPerSec = getLatestValue(metrics.getRouterRequestsTotal());
            flattened.put("requestsPerSec", requestsPerSec);
            flattened.put("avgLatency", getLatestValue(metrics.getRouterRequestsDuration()));
            flattened.put("activeConnections", (int) getLatestValue(metrics.getRouterActiveConnections()));
            flattened.put("workersHealthy", (int) getLatestValue(metrics.getRouterWorkerHealth()));
            flattened.put("averageWorkerLoad", getLatestValue(metrics.getRouterWorkerLoad()));
            flattened.put("errorRate", getLatestValue(metrics.getRouterErrorRate()));
            flattened.put("timeoutRate", getLatestValue(metrics.getRouterTimeoutRate()));
            flattened.put("cpuUsage", getLatestValue(metrics.getRouterCpuUsage()));
            flattened.put("memoryUsage", getLatestValue(metrics.getRouterMemoryUsage()));
            flattened.put("diskUsage", getLatestValue(metrics.getRouterDiskUsage()));
            flattened.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            log.warn("Error flattening router metrics: {}", e.getMessage());
            // Return safe defaults on error
            flattened.put("requestsPerSec", 0.0);
            flattened.put("avgLatency", 0.0);
            flattened.put("activeConnections", 0);
            flattened.put("workersHealthy", 0);
            flattened.put("averageWorkerLoad", 0.0);
            flattened.put("errorRate", 0.0);
            flattened.put("timeoutRate", 0.0);
            flattened.put("cpuUsage", 0.0);
            flattened.put("memoryUsage", 0.0);
            flattened.put("diskUsage", 0.0);
            flattened.put("timestamp", System.currentTimeMillis());
        }

        return flattened;
    }

    private double getLatestValue(Map<Double, BigDecimal> metricData) {
        if (metricData == null || metricData.isEmpty()) {
            return 0.0;
        }

        return metricData.entrySet().stream()
            .max(Map.Entry.comparingByKey())
            .map(entry -> entry.getValue().doubleValue())
            .orElse(0.0);
    }
}
