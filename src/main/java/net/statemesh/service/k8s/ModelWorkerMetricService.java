package net.statemesh.service.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.flow.CreateModelFlow;
import net.statemesh.k8s.util.MetricType;
import net.statemesh.k8s.util.Metrics;
import net.statemesh.service.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

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
public class ModelWorkerMetricService extends PollingEventStreamService {
    private final ObjectMapper objectMapper;

    public ModelWorkerMetricService(
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

            long startTime = Instant.now().atZone(ZoneOffset.UTC).minusMinutes(60).toEpochSecond() * 1000;
            long endTime = Instant.now().atZone(ZoneOffset.UTC).toEpochSecond() * 1000;

            var result = kubernetesController.readMetrics(
                application.getProject().getNamespace(),
                application.getProject().getCluster(),
                MetricType.MODEL_WORKER,
                null,
                routerApp,
                null,
                null,
                podName,
                startTime,
                endTime,
                pollInterval
            ).get(READ_METRICS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.isSuccess()) {
                log.error("Metrics read failed for application {} pod {} container {}",
                    applicationId, podName, containerName);
                if (!Thread.currentThread().isInterrupted()) {
                    return;
                }
            }

            var metrics = result.getValue();
            if (metrics == null) {
                log.warn("Metrics value is null!");
                return;
            }

            var flattenedMetrics = flattenWorkerMetrics(metrics);
            sendEvent("metrics", flattenedMetrics, emId);

        } catch (ExecutionException e) {
            log.error("ExecutionException in model worker metrics for application {}: {}",
                applicationId, e.getMessage(), e);
        }
    }

    private Map<String, Object> flattenWorkerMetrics(Metrics metrics) {
        Map<String, Object> flattened = new HashMap<>();

        try {
            double requestsRunning = getLatestValue(metrics.getModelRequestsRunning());
            double requestsWaiting = getLatestValue(metrics.getModelRequestsWaiting());
            double kvCacheUsage = getLatestValue(metrics.getModelKvCacheUsage());
            double promptTokensPerSec = getLatestValue(metrics.getModelPromptTokensRate());
            double generationTokensPerSec = getLatestValue(metrics.getModelGenerationTokensRate());
            double timeToFirstToken = getLatestValue(metrics.getModelTimeToFirstToken());
            double timePerOutputToken = getLatestValue(metrics.getModelTimePerOutputToken());
            double prefillTime = getLatestValue(metrics.getModelPrefillTime());
            double decodeTime = getLatestValue(metrics.getModelDecodeTime());

            flattened.put("requestsRunning", (int) requestsRunning);
            flattened.put("requestsWaiting", (int) requestsWaiting);
            flattened.put("kvCacheUsage", kvCacheUsage);
            flattened.put("promptTokensPerSec", promptTokensPerSec);
            flattened.put("generationTokensPerSec", generationTokensPerSec);
            flattened.put("timeToFirstToken", timeToFirstToken);
            flattened.put("timePerOutputToken", timePerOutputToken);
            flattened.put("prefillTime", prefillTime);
            flattened.put("decodeTime", decodeTime);
            flattened.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Error flattening worker metrics: {}", e.getMessage(), e);
            // Return safe defaults on error
            flattened.put("requestsRunning", 0);
            flattened.put("requestsWaiting", 0);
            flattened.put("kvCacheUsage", 0.0);
            flattened.put("promptTokensPerSec", 0.0);
            flattened.put("generationTokensPerSec", 0.0);
            flattened.put("timeToFirstToken", 0.0);
            flattened.put("timePerOutputToken", 0.0);
            flattened.put("prefillTime", 0.0);
            flattened.put("decodeTime", 0.0);
            flattened.put("timestamp", System.currentTimeMillis());

            log.warn("Using default values due to error");
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
