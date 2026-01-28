package net.statemesh.k8s.api;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.api.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AimClient {
    private static final String EXPERIMENTS_ENDPOINT = "/api/experiments/";
    private static final String EXPERIMENTS_RUNS_PATH = "/runs/";
    private static final String BATCH_METRICS_ENDPOINT = "/api/runs/";
    private static final String BATCH_METRICS_PATH = "/metric/get-batch/";
    public static final AimContext DEFAULT_CONTEXT = new AimContext(Collections.singletonMap("phase", "train"));
    public static final AimContext DEFAULT_SUROGATE_CONTEXT = new AimContext(Collections.emptyMap());
    public static final List<String> DEFAULT_METRICS = List.of(
        "epoch",
        "grad_norm",
        "learning_rate",
        "loss",
        "tokens_per_second_per_gpu",
        "eval_loss"
    );
    public static final List<String> DEFAULT_SUROGATE_METRICS = List.of(
        "train/epoch",
        "train/norm",
        "train/lr",
        "train/loss",
        "train/tokens_per_second",
        "eval/loss"
    );

    private final RestTemplate restTemplate;
    private final ApplicationProperties.RayCluster config;

    public AimClient(RestTemplate restTemplate, ApplicationProperties.RayCluster config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public String name() {
        return config.getName();
    }

    public List<AimMetric> getExperimentMetric(String experimentName, List<String> metricNames, AimContext context) {
        final String experimentId = getExperiments().stream()
            .filter(aimExperiment -> experimentName.equals(aimExperiment.getName()))
            .map(AimExperiment::getId)
            .findAny()
            .orElse(null);
        if (experimentId == null) {
            log.error("Experiment {} was not found", experimentName);
            return null;
        }

        final String runId = getExperimentRuns(experimentId).getRuns().stream()
            .map(AimRun::getRunId)
            .findFirst()
            .orElse(null);
        if (runId == null) {
            log.error("No run for experiment {} was found", experimentName);
            return null;
        }

        return getRunBatchMetric(runId, metricNames, context);
    }

    public List<AimMetric> getRunBatchMetric(String runId, List<String> metricNames, AimContext context) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<?> entity = new HttpEntity<>(
            metricNames.stream()
                .map(metric ->
                    AimMetricRequest.builder()
                        .name(metric)
                        .context(context)
                        .build()
                )
                .collect(Collectors.toList()),
            headers
        );

        ResponseEntity<List<AimMetric>> response = restTemplate.exchange(
            StringUtils.join(
                config.getAimUrl(),
                BATCH_METRICS_ENDPOINT,
                runId,
                BATCH_METRICS_PATH
            ),
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<>() {}
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Empty response from Aim server");
        }

        return response.getBody();
    }

    public AimExperiment getExperimentRuns(String experimentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<AimExperiment> response = restTemplate.exchange(
            StringUtils.join(
                config.getAimUrl(),
                EXPERIMENTS_ENDPOINT,
                experimentId,
                EXPERIMENTS_RUNS_PATH
            ),
            HttpMethod.GET,
            entity,
            AimExperiment.class
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Empty response from Aim server");
        }

        return response.getBody();
    }

    public List<AimExperiment> getExperiments() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<List<AimExperiment>> response = restTemplate.exchange(
            StringUtils.join(
                config.getAimUrl(),
                EXPERIMENTS_ENDPOINT
            ),
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<>() {}
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Empty response from Aim server");
        }

        return response.getBody();
    }
}
