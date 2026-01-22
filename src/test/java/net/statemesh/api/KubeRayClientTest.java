package net.statemesh.api;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.IntegrationTest;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.api.KubeRayClient;
import net.statemesh.k8s.api.model.RayJobDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

@IntegrationTest
@Slf4j
public class KubeRayClientTest {
    static final String SUBMISSION_ID = "my-job-1-wjgvg";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Test
    void testJobDetails() {
        if (applicationProperties.getProfile().getRayClusters() == null){
            throw new RuntimeException("No RayCluster api configured");
        }

        KubeRayClient kubeRayClient = new KubeRayClient(
            restTemplate,
            applicationProperties.getProfile().getRayClusters()
                .stream().findAny()
                .orElseThrow(() -> new RuntimeException("No RayCluster api configured"))
        );

        log.info(
            kubeRayClient.getJobDetails(SUBMISSION_ID).toString()
        );
    }

    @Test
    void testLogs() {
        if (applicationProperties.getProfile().getRayClusters() == null){
            throw new RuntimeException("No RayCluster api configured");
        }

        KubeRayClient kubeRayClient = new KubeRayClient(
            restTemplate,
            applicationProperties.getProfile().getRayClusters()
                .stream().findAny()
                .orElseThrow(() -> new RuntimeException("No RayCluster api configured"))
        );

        RayJobDetails details = kubeRayClient.getJobDetails(SUBMISSION_ID);

        log.info(
            kubeRayClient.getLogs(SUBMISSION_ID, details.getDriverAgentNodeId(), 100)
        );
    }
}
