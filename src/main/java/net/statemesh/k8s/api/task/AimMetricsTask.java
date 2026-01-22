package net.statemesh.k8s.api.task;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.api.AimClient;
import net.statemesh.k8s.api.model.AimMetric;
import net.statemesh.k8s.exception.APIException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.Metrics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class AimMetricsTask {
    private final ApiStub apiStub;
    private final String rayCluster;
    private final String jobId;

    public AimMetricsTask(ApiStub apiStub,
                          String rayCluster,
                          String jobId) {
        this.apiStub = apiStub;
        this.rayCluster = rayCluster;
        this.jobId = jobId;
    }

    public CompletableFuture<TaskResult<Metrics>> call() {
        if (!this.apiStub.getAimClients().containsKey(rayCluster)) {
            throw new APIException("Aim client for ray cluster " + rayCluster + " was not configured");
        }
        final AimClient client = this.apiStub.getAimClients().get(rayCluster);
        final List<AimMetric> metrics = client.getExperimentMetric(jobId, AimClient.DEFAULT_METRICS);

        return CompletableFuture.completedFuture(
            TaskResult.<Metrics>builder()
                .success(true)
                .value(
                    Metrics.builder()
                        .epoch(parseMetricData(metrics, "epoch"))
                        .loss(parseMetricData(metrics, "loss"))
                        .evalLoss(parseMetricData(metrics, "eval_loss"))
                        .gradNorm(parseMetricData(metrics, "grad_norm"))
                        .learningRate(parseMetricData(metrics, "learning_rate"))
                        .tokensPerSecondPerGpu(parseMetricData(metrics, "tokens_per_second_per_gpu"))
                        .created(Instant.now())
                        .build()
                )
                .build()
        );
    }

    private Map<Double, BigDecimal> parseMetricData(List<AimMetric> metrics, String name) {
        if (metrics == null) {
            return null;
        }
        var metric = metrics.stream().filter(m -> name.equals(m.getName())).findAny().orElse(null);
        if (metric == null) {
            return null;
        }

        Map<Double, BigDecimal> result = new HashMap<>();
        int idx = 0;
        for (var iter: metric.getIters()) {
            result.put(iter, BigDecimal.valueOf(metric.getValues().get(idx++)));
        }
        return result;
    }
}
