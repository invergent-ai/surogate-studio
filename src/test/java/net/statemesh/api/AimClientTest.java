package net.statemesh.api;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.IntegrationTest;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.api.AimClient;
import net.statemesh.k8s.api.model.AimExperiment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

@IntegrationTest
@Slf4j
public class AimClientTest {
    static final String EXPERIMENT_NAME = "my-job-1";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Test
    void testExperimentMetric() {
        if (applicationProperties.getProfile().getRayClusters() == null){
            throw new RuntimeException("No Aim api configured");
        }

        AimClient aimClient = new AimClient(
            restTemplate,
            applicationProperties.getProfile().getRayClusters()
                .stream().findAny()
                .orElseThrow(() -> new RuntimeException("No Aim api configured"))
        );

        log.info("Experiment Metric {}",
            aimClient.getExperimentMetric(EXPERIMENT_NAME, AimClient.DEFAULT_METRICS)
        );
    }

    @Test
    void testExperimentRuns() {
        if (applicationProperties.getProfile().getRayClusters() == null){
            throw new RuntimeException("No Aim api configured");
        }

        AimClient aimClient = new AimClient(
            restTemplate,
            applicationProperties.getProfile().getRayClusters()
                .stream().findAny()
                .orElseThrow(() -> new RuntimeException("No Aim api configured"))
        );

        final String experimentId = aimClient.getExperiments().stream()
            .filter(aimExperiment -> EXPERIMENT_NAME.equals(aimExperiment.getName()))
            .map(AimExperiment::getId)
            .findAny()
            .orElse(null);
        if (experimentId == null) {
            log.error("Experiment {} was not found", EXPERIMENT_NAME);
            throw new RuntimeException("Experiment " + EXPERIMENT_NAME + " was not found");
        }

        log.info("Experiment {}",
            aimClient.getExperimentRuns(experimentId)
        );
    }
}
