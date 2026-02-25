package net.statemesh.service.ghcr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GhcrService {
    private static final String GHCR_REGISTRY = "https://ghcr.io/v2";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getTags(String owner, String imageName) {
        if (imageName.contains(":")) {
            imageName = imageName.substring(0, imageName.indexOf(":"));
        }

        try {
            String token = getAnonymousToken(owner, imageName);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            String url = String.format("%s/%s/%s/tags/list", GHCR_REGISTRY, owner, imageName);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : Map.of("tags", Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to fetch tags for ghcr.io/{}/{}: {}", owner, imageName, e.getMessage());
            throw new RuntimeException("Failed to fetch tags", e);
        }
    }

    public Map<String, Object> getImageConfig(String owner, String imageName, String tag) {
        if (imageName.contains(":")) {
            imageName = imageName.substring(0, imageName.indexOf(":"));
        }

        try {
            String token = getAnonymousToken(owner, imageName);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", String.join(", ",
                "application/vnd.docker.distribution.manifest.v2+json",
                "application/vnd.oci.image.manifest.v1+json",
                "application/vnd.oci.image.index.v1+json"
            ));

            String manifestUrl = String.format("%s/%s/%s/manifests/%s", GHCR_REGISTRY, owner, imageName, tag);
            ResponseEntity<Map<String, Object>> manifestResponse = restTemplate.exchange(
                manifestUrl, HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> manifestBody = manifestResponse.getBody();
            if (manifestBody == null) {
                return Map.of("ports", Collections.emptyList(), "volumes", Collections.emptyList());
            }

            // Handle OCI index (multi-platform) — pick first manifest
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> manifests = (List<Map<String, Object>>) manifestBody.get("manifests");
            if (manifests != null && !manifests.isEmpty()) {
                // Pick first amd64/linux manifest, or just first
                Map<String, Object> selected = manifests.stream()
                    .filter(m -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> platform = (Map<String, Object>) m.get("platform");
                        return platform != null
                            && "linux".equals(platform.get("os"))
                            && "amd64".equals(platform.get("architecture"));
                    })
                    .findFirst()
                    .orElse(manifests.get(0));

                String digest = (String) selected.get("digest");

                // Fetch the actual manifest
                HttpHeaders innerHeaders = new HttpHeaders();
                innerHeaders.set("Authorization", "Bearer " + token);
                innerHeaders.set("Accept", String.join(", ",
                    "application/vnd.docker.distribution.manifest.v2+json",
                    "application/vnd.oci.image.manifest.v1+json"
                ));

                String innerUrl = String.format("%s/%s/%s/manifests/%s", GHCR_REGISTRY, owner, imageName, digest);
                ResponseEntity<Map<String, Object>> innerResponse = restTemplate.exchange(
                    innerUrl, HttpMethod.GET, new HttpEntity<>(innerHeaders),
                    new ParameterizedTypeReference<>() {}
                );
                manifestBody = innerResponse.getBody();
                if (manifestBody == null) {
                    return Map.of("ports", Collections.emptyList(), "volumes", Collections.emptyList());
                }
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) manifestBody.get("config");
            String digest = (String) config.get("digest");

            HttpHeaders blobHeaders = new HttpHeaders();
            blobHeaders.set("Authorization", "Bearer " + token);
            String blobUrl = String.format("%s/%s/%s/blobs/%s", GHCR_REGISTRY, owner, imageName, digest);
            ResponseEntity<byte[]> blobResponse = restTemplate.exchange(
                blobUrl, HttpMethod.GET, new HttpEntity<>(blobHeaders), byte[].class
            );

            Map<String, Object> blobBody = objectMapper.readValue(blobResponse.getBody(), new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            Map<String, Object> containerConfig = (Map<String, Object>) blobBody.get("config");

            return extractPortsAndVolumes(containerConfig);
        } catch (Exception e) {
            log.error("Failed to get image config for ghcr.io/{}/{}: {}", owner, imageName, e.getMessage(), e);
            return Map.of("ports", Collections.emptyList(), "volumes", Collections.emptyList());
        }
    }

    private String getAnonymousToken(String owner, String imageName) {
        String scope = String.format("repository:%s/%s:pull", owner, imageName);
        String tokenUrl = String.format("https://ghcr.io/token?service=ghcr.io&scope=%s", scope);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            tokenUrl, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()),
            new ParameterizedTypeReference<>() {}
        );
        return (String) response.getBody().get("token");
    }

    private Map<String, Object> extractPortsAndVolumes(Map<String, Object> containerConfig) {
        List<Map<String, Object>> ports = new ArrayList<>();
        List<String> volumePaths = new ArrayList<>();

        if (containerConfig != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> exposedPorts = (Map<String, Object>) containerConfig.get("ExposedPorts");
            if (exposedPorts != null) {
                ports = exposedPorts.keySet().stream()
                    .map(port -> {
                        String[] parts = port.split("/");
                        Map<String, Object> portInfo = new HashMap<>();
                        portInfo.put("containerPort", Integer.parseInt(parts[0]));
                        portInfo.put("protocol", parts.length > 1 ? parts[1].toUpperCase() : "TCP");
                        return portInfo;
                    })
                    .collect(Collectors.toList());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> volumes = (Map<String, Object>) containerConfig.get("Volumes");
            if (volumes != null) {
                volumePaths = new ArrayList<>(volumes.keySet());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("ports", ports);
        result.put("volumes", volumePaths);
        return result;
    }
}
