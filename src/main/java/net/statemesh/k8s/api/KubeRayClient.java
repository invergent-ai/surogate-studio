package net.statemesh.k8s.api;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.api.model.RayJobDetails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
public class KubeRayClient {
    private static final String JOBS_ENDPOINT = "/api/jobs/";
    private static final String LOGS_ENDPOINT = "/api/v0/logs/file";

    private final RestTemplate restTemplate;
    private final ApplicationProperties.RayCluster config;

    public KubeRayClient(RestTemplate restTemplate, ApplicationProperties.RayCluster config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public String name() {
        return config.getName();
    }

    public RayJobDetails getJobDetails(String submissionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<List<RayJobDetails>> response = restTemplate.exchange(
            StringUtils.join(
                config.getUrl(),
                JOBS_ENDPOINT
            ),
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<>() {}
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Empty response from Ray server");
        }

        return response.getBody().stream()
            .filter(details -> submissionId.equals(details.getSubmissionId()))
            .findFirst()
            .orElse(null);
    }

    public String getLogs(String submissionId, String nodeId, int lines) {
        ResponseEntity<String> response = restTemplate.exchange(
            StringUtils.join(
                config.getUrl(),
                LOGS_ENDPOINT,
                "?node_id=", nodeId,
                "&filename=job-driver-", submissionId, ".log",
                "&lines=", lines
            ),
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            String.class
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Empty response from Ray server");
        }

        return response.getBody();
    }
}
