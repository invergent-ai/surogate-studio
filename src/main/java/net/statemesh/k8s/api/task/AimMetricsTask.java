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

import static net.statemesh.k8s.api.AimClient.*;

@Slf4j
public class AimMetricsTask {
    private final ApiStub apiStub;
    private final String rayCluster;
    private final String jobId;
    private final Boolean useAxolotl;

    public AimMetricsTask(ApiStub apiStub,
                          String rayCluster,
                          String jobId,
                          Boolean useAxolotl) {
        this.apiStub = apiStub;
        this.rayCluster = rayCluster;
        this.jobId = jobId;
        this.useAxolotl = useAxolotl;
    }

    public CompletableFuture<TaskResult<Metrics>> call() {
        if (!this.apiStub.getAimClients().containsKey(rayCluster)) {
            throw new APIException("Aim client for ray cluster " + rayCluster + " was not configured");
        }
        final AimClient client = this.apiStub.getAimClients().get(rayCluster);
        final List<AimMetric> metrics = client.getExperimentMetric(jobId,
            Boolean.TRUE.equals(useAxolotl) ? DEFAULT_METRICS : DEFAULT_SUROGATE_METRICS,
            Boolean.TRUE.equals(useAxolotl) ? DEFAULT_CONTEXT : DEFAULT_SUROGATE_CONTEXT);

        return CompletableFuture.completedFuture(
            TaskResult.<Metrics>builder()
                .success(true)
                .value(
                    Boolean.TRUE.equals(useAxolotl) ? metrics(metrics) : surogateMetrics(metrics)
                )
                .build()
        );
    }

    private Metrics metrics(List<AimMetric> metrics) {
       return Metrics.builder()
           .epoch(parseMetricData(metrics, "epoch"))
           .loss(parseMetricData(metrics, "loss"))
           .evalLoss(parseMetricData(metrics, "eval_loss"))
           .gradNorm(parseMetricData(metrics, "grad_norm"))
           .learningRate(parseMetricData(metrics, "learning_rate"))
           .tokensPerSecondPerGpu(parseMetricData(metrics, "tokens_per_second_per_gpu"))
           .created(Instant.now())
           .build();
    }

    private Metrics surogateMetrics(List<AimMetric> metrics) {
        return Metrics.builder()
            .epoch(parseMetricData(metrics, "train/epoch"))
            .loss(parseMetricData(metrics, "train/loss"))
            .evalLoss(parseMetricData(metrics, "eval/loss"))
            .gradNorm(parseMetricData(metrics, "train/norm"))
            .learningRate(parseMetricData(metrics, "train/lr"))
            .tokensPerSecondPerGpu(parseMetricData(metrics, "train/tokens_per_second"))
            .created(Instant.now())
            .build();
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
