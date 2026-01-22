package net.statemesh.service.docker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.util.ApiUtils;
import net.statemesh.k8s.util.DockerSecret;
import net.statemesh.service.ApplicationService;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.DockerHubSearchResponse;
import net.statemesh.service.dto.DockerHubTagResponse;
import net.statemesh.service.dto.RegistryValidationRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static javax.management.timer.Timer.ONE_DAY;
import static net.statemesh.k8s.util.K8SConstants.SECRET_DOCKER_DATA_KEY;
import static net.statemesh.k8s.util.NamingUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DockerHubService {
    private static final String DOCKER_HUB_API_BASE = "https://hub.docker.com";
    private static final String DOCKER_HUB_SEARCH_URL = DOCKER_HUB_API_BASE + "/api/search/v3/catalog/search";
    private static final String DOCKER_HUB_TAGS_URL = DOCKER_HUB_API_BASE + "/v2/repositories/{imageName}/tags/";
    private static final String DOCKER_HUB_IMAGE_CONFIG_URL = DOCKER_HUB_API_BASE + "/v2/repositories/{imageName}/tags/{tag}/images";

    private static final String DOCKER_AUTH_URL = "https://auth.docker.io/token";
    private static final String DOCKER_REGISTRY_URL = "https://registry-1.docker.io/v2";
    private static final String DOCKER_HUB_URL = "https://registry.hub.docker.com/v2";
    private static final String DOCKER_REGISTRY_SERVICE = "registry.docker.io";

    private static final int RESULTS_PER_PAGE = 25;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KubernetesController kubernetesController;
    private final ApplicationService applicationService;

    @Cacheable("publicRepos")
    public DockerHubSearchResponse searchImages(String query) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(DOCKER_HUB_SEARCH_URL)
                .queryParam("query", query != null ? query : "")
                .queryParam("page_size", RESULTS_PER_PAGE)
                .queryParam("type", "image");

            ResponseEntity<DockerHubSearchResponse> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                DockerHubSearchResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to search Docker Hub", e);
            throw new RuntimeException("Failed to search Docker Hub", e);
        }
    }

    @Cacheable("imageTags")
    public DockerHubTagResponse getPublicImageTags(String imageName) {
        if (imageName == null || imageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Image name cannot be empty");
        }

        try {
            // Handle official images by adding library/ prefix
            String adjustedImageName = !imageName.contains("/") ? "library/" + imageName : imageName;

            String url = UriComponentsBuilder.fromUriString(DOCKER_HUB_TAGS_URL)
                .queryParam("page_size", 100)
                .queryParam("ordering", "last_updated")
                .buildAndExpand(adjustedImageName)
                .toUriString();

            log.debug("Requesting URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Java/SpringBoot");

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<DockerHubTagResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                DockerHubTagResponse.class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from Docker Hub");
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch Docker Hub tags for image: {}. URL: {}. Error: {}",
                imageName,
                DOCKER_HUB_TAGS_URL.replace("{imageName}", imageName),
                e.getMessage());
            throw new RuntimeException("Failed to fetch Docker Hub tags for " + imageName);
        }
    }

    @Cacheable("imageConfigs")
    public Map<String, Object> getImageConfig(String imageName, String tag) {
        try {
            String adjustedImageName = adjustImageName(imageName);
            String url = DOCKER_HUB_IMAGE_CONFIG_URL
                .replace("{imageName}", adjustedImageName)
                .replace("{tag}", tag);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                Optional<Map<String, Object>> imageInfo = response.getBody().stream()
                    .filter(img -> "amd64".equals(img.get("architecture")))
                    .findFirst();

                if (imageInfo.isEmpty()) {
                    imageInfo = response.getBody().stream().findFirst();
                }

                if (imageInfo.isPresent()) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> layers = (List<Map<String, Object>>) imageInfo.get().get("layers");

                    if (layers != null) {
                        Map<String, Object> exposedPorts = new HashMap<>();
                        Map<String, Object> volumes = new HashMap<>();

                        layers.forEach(layer -> {
                            String instruction = (String) layer.get("instruction");
                            if (instruction != null) {
                                // Handle EXPOSE instructions
                                if (instruction.startsWith("EXPOSE")) {
                                    String[] parts = instruction.split("\\s+");
                                    for (int i = 1; i < parts.length; i++) {
                                        String port = parts[i];
                                        if (!port.contains("/")) {
                                            port += "/tcp"; // Default to TCP if protocol not specified
                                        }
                                        exposedPorts.put(port, new HashMap<>());
                                    }
                                }
                                // Handle VOLUME instructions
                                else if (instruction.startsWith("VOLUME")) {
                                    String volumeStr = instruction.substring("VOLUME".length()).trim();
                                    List<String> volumePaths = new ArrayList<>();

                                    if (volumeStr.startsWith("[")) {
                                        // Handle JSON array format: VOLUME ["/data", "/var/log"]
                                        try {
                                            // Remove brackets and split by comma
                                            volumeStr = volumeStr.substring(1, volumeStr.length() - 1);
                                            Arrays.stream(volumeStr.split(","))
                                                .map(String::trim)
                                                .map(s -> s.replace("\"", ""))
                                                .forEach(volumePaths::add);
                                        } catch (Exception e) {
                                            log.warn("Failed to parse JSON volume format: {}", volumeStr);
                                        }
                                    } else {
                                        // Handle space-separated format: VOLUME /var/log /var/db
                                        volumePaths.addAll(Arrays.asList(volumeStr.split("\\s+")));
                                    }

                                    volumePaths.forEach(path -> volumes.put(path, new HashMap<>()));
                                }
                            }
                        });

                        Map<String, Object> result = new HashMap<>();
                        if (!exposedPorts.isEmpty()) {
                            result.put("ExposedPorts", exposedPorts);
                        }
                        if (!volumes.isEmpty()) {
                            result.put("Volumes", volumes);
                        }
                        return result;
                    }
                }
            }
            return Map.of();
        } catch (Exception e) {
            log.error("Failed to fetch image config for {}, tag {}", imageName, tag, e);
            return Map.of();
        }
    }

    private String adjustImageName(String imageName) {
        if (!imageName.contains("/")) {
            return "library/" + imageName;
        }
        // Handle organization images (e.g., bitnami/postgresql)
        if (imageName.split("/").length == 2 && !imageName.contains(".")) {
            return imageName;
        }
        // Handle fully qualified images (e.g., registry.hub.docker.com/bitnami/postgresql)
        return imageName.substring(imageName.indexOf("/") + 1);
    }

    public void validateCredentials(RegistryValidationRequest request) {
        if (request.getUsername() == null && request.getPassword() == null) {
            throw new RuntimeException("Missing credentials");
        }

        if (!secretExists(request)) {
            if (isCustomRegistry(request.getUrl())) {
                validateCustomRegistryCredentials(request);
            } else {
                // Assume Docker Hub
                validateDockerHubCredentials(request);
            }
        }
    }

    private boolean secretExists(RegistryValidationRequest request) {
        if (!StringUtils.isEmpty(request.getApplicationId()) && !StringUtils.isEmpty(request.getImageName())) {
            ApplicationDTO application = applicationService.findOne(request.getApplicationId()).orElse(null);

            if (application != null && application.getProject() != null
                && application.getProject().getCluster() != null
                && !StringUtils.isEmpty(application.getDeployedNamespace())) {

                Map<String, byte[]> secret = ApiUtils.readSecret(
                    this.kubernetesController.getClients()
                        .get(application.getProject().getZone().getZoneId())
                        .get(application.getProject().getCluster().getCid()),
                    application.getDeployedNamespace(),
                    dockerSecretName(containerName(application.getInternalName(), request.getImageName()))
                );
                if (secret != null && secret.containsKey(SECRET_DOCKER_DATA_KEY)) {
                    final byte[] dockerJson = secret.get(SECRET_DOCKER_DATA_KEY);
                    if (dockerJson != null) {
                        try {
                            DockerSecret dockerSecret = objectMapper.readValue(dockerJson, DockerSecret.class);
                            if (dockerSecret.getAuths().containsKey(request.getUrl())) {
                                final DockerSecret.Auth auth = dockerSecret.getAuths().get(request.getUrl());
                                if (request.getUsername().equals(auth.getUsername())) {
                                    return Boolean.TRUE;
                                }
                            }
                        } catch (IOException e) {
                            log.debug("Could not decode docker secret with message {}", e.getMessage());
                        }
                    }
                }
            }
        }

        return Boolean.FALSE;
    }

    private void validateDockerHubCredentials(RegistryValidationRequest request) {
        try {
            ResponseEntity<?> response = call(
                String.format("%s?service=%s&scope=repository:%s:pull", DOCKER_AUTH_URL, DOCKER_REGISTRY_SERVICE, request.getUrl()),
                request.getUsername(), request.getPassword(), null
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to validate Docker Hub registry access");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("Invalid Docker Hub credentials", e);
            }
            throw new RuntimeException("Error validating Docker Hub registry: " + e.getMessage(), e);
        }
    }

    private void validateCustomRegistryCredentials(RegistryValidationRequest request) {
        try {
            ResponseEntity<Void> response = call(
                request.getUrl().contains("/v2/_catalog") ? request.getUrl() :
                    StringUtils.join(request.getUrl(), "/v2/_catalog"),
                request.getUsername(), request.getPassword());
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to validate custom registry access");
            }
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("Invalid credentials for custom registry", e);
            }
            throw new RuntimeException("Error validating custom registry: " + e.getMessage(), e);
        }
    }

    public DockerHubSearchResponse searchImagesPrivateRepo(String url, String username, String password) {
        if (username == null && password == null) {
            throw new RuntimeException("Missing credentials");
        }
        return searchPrivateRegistry(url, username, password);
    }

    public Map<String, Object> getPrivateRegistryImageDigestConfig(
        String registryUrl,
        String imageName,
        String tag,
        String registryUser,
        String registryPassword
    ) {
        try {
            // Remove /v2/_catalog from registry URL if present
            final String baseRegistryUrl = registryUrl.contains("/v2/_catalog") ?
                registryUrl : registryUrl.replace("/v2/_catalog", "");
            ResponseEntity<Map<String, Object>> manifestResponse = call(
                String.format("%s/v2/%s/manifests/%s", baseRegistryUrl, imageName, tag),
                registryUser, registryPassword, Map.of("Accept", "application/vnd.docker.distribution.manifest.v2+json")
            );

            // Extract digest from config object in manifest
            Map<String, Object> manifestBody = manifestResponse.getBody();
            if (manifestBody == null) {
                throw new RuntimeException("Failed to get manifest for image");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) manifestBody.get("config");
            String digest = (String) config.get("digest");

            // Configure headers for blob request
            HttpHeaders blobHeaders = new HttpHeaders();
            blobHeaders.set("Authorization", createBasicAuthHeader(registryUser, registryPassword));
            blobHeaders.set("Accept", "application/json");

            // Second request to get configuration using digest
            String blobUrl = String.format("%s/v2/%s/blobs/%s", baseRegistryUrl, imageName, digest);
            HttpEntity<?> blobRequestEntity = new HttpEntity<>(blobHeaders);

            ResponseEntity<byte[]> blobResponse = restTemplate.exchange(
                blobUrl,
                HttpMethod.GET,
                blobRequestEntity,
                byte[].class
            );

            // Convert byte array response to Map
            Map<String, Object> blobBody = objectMapper.readValue(blobResponse.getBody(), new TypeReference<>() {});

            // Extract config data
            @SuppressWarnings("unchecked")
            Map<String, Object> containerConfig = (Map<String, Object>) blobBody.get("config");

            // Prepare response with ports and volumes
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> ports = new ArrayList<>();
            List<String> volumePaths = new ArrayList<>();

            // Map ExposedPorts
            @SuppressWarnings("unchecked")
            Map<String, Object> exposedPorts = (Map<String, Object>) containerConfig.get("ExposedPorts");
            if (exposedPorts != null) {
                ports = exposedPorts.keySet().stream()
                    .map(port -> {
                        String[] parts = port.split("/");
                        Map<String, Object> portInfo = new HashMap<>();
                        portInfo.put("containerPort", Integer.parseInt(parts[0]));
                        portInfo.put("protocol", parts[1].toUpperCase());
                        return portInfo;
                    })
                    .collect(Collectors.toList());
            }

            // Map Volumes
            @SuppressWarnings("unchecked")
            Map<String, Object> volumes = (Map<String, Object>) containerConfig.get("Volumes");
            if (volumes != null) {
                volumePaths = new ArrayList<>(volumes.keySet());
            }

            result.put("ports", ports);
            result.put("volumes", volumePaths);

            return result;

        } catch (Exception e) {
            log.error("Error getting image config with digest: {}", e.getMessage(), e);
            return Map.of("ports", Collections.emptyList(), "volumes", Collections.emptyList());
        }
    }

    public Map<String, Object> getPrivateRegistryImageConfig(
        String registryUrl,
        String namespace,
        String imageName,
        String tag,
        String username,
        String password
    ) {
        try {
            ResponseEntity<Map<String, Object>> tokenResponse = call(
                String.format("%s?service=%s&scope=repository:%s:pull",
                    DOCKER_AUTH_URL, DOCKER_REGISTRY_SERVICE, registryUrl.contains("/") ? registryUrl : registryUrl + "/" + imageName),
                username, password, null
            );

            final String token = (String) Objects.requireNonNull(tokenResponse.getBody()).get("token");
            final Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "Accept", "application/vnd.docker.distribution.manifest.v2+json"
            );

            ResponseEntity<Map<String, Object>> manifestResponse =
                call(String.format("%s/%s/%s/manifests/%s",
                    DOCKER_REGISTRY_URL,
                    namespace,
                    imageName,
                    tag
                ), null, null, headers);

            if (manifestResponse.getBody() != null) {
                Map<String, Object> config = (Map<String, Object>) manifestResponse.getBody().get("config");
                String configDigest = (String) config.get("digest");

                ResponseEntity<String> configResponse = call(
                    String.format("%s/%s/%s/blobs/%s",
                        DOCKER_REGISTRY_URL,
                        namespace,
                        imageName,
                        configDigest
                    ), headers);

                Map<String, Object> configBody = objectMapper.readValue(configResponse.getBody(), Map.class);
                Map<String, Object> dockerConfig = (Map<String, Object>) configBody.get("config");

                if (dockerConfig != null) {
                    Map<String, Object> exposedPorts = (Map<String, Object>) dockerConfig.get("ExposedPorts");
                    Map<String, Object> volumes = (Map<String, Object>) dockerConfig.get("Volumes");

                    Map<String, Object> result = new HashMap<>();
                    List<Map<String, Object>> ports = new ArrayList<>();
                    List<String> volumePaths = new ArrayList<>();

                    if (exposedPorts != null) {
                        ports = exposedPorts.keySet().stream()
                            .map(port -> {
                                String[] parts = port.split("/");
                                Map<String, Object> portInfo = new HashMap<>();
                                portInfo.put("containerPort", Integer.parseInt(parts[0]));
                                portInfo.put("protocol", parts[1].toUpperCase());
                                return portInfo;
                            })
                            .collect(Collectors.toList());
                    }

                    if (volumes != null) {
                        volumePaths = new ArrayList<>(volumes.keySet());
                    }

                    result.put("ports", ports);
                    result.put("volumes", volumePaths);

                    return result;
                }
            }
            return Map.of();
        } catch (Exception e) {
            log.error("Failed to fetch private registry image config", e);
            return Map.of();
        }
    }

    public Map<String, Object> getPrivateRegistryImageTags(String url, String username, String password) {
        ResponseEntity<Map<String, Object>> response = call(
            String.format("%s/repositories/%s/tags", DOCKER_HUB_URL, url), username, password, null);

        Map<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("count", Objects.requireNonNull(response.getBody()).get("count"));
        responseMap.put("next", response.getBody().get("next"));
        responseMap.put("previous", response.getBody().get("previous"));
        responseMap.put("results", response.getBody().get("results"));

        return responseMap;
    }

    private DockerHubSearchResponse searchPrivateRegistry(String url, String username, String password) {
        if (isCustomRegistry(url)) {
            return searchCustomRegistry(url, username, password);
        } else {
            return searchDockerHubRegistry(url, username, password);
        }
    }

    private DockerHubSearchResponse searchDockerHubRegistry(String query, String username, String password) {
        // Docker Hub API authentication
        String authUrl = "https://hub.docker.com/v2/users/login/";
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);
        HttpEntity<Map<String, String>> authRequest = new HttpEntity<>(credentials, authHeaders);
        ResponseEntity<Map<String, Object>> authResponse = restTemplate.exchange(
            authUrl, HttpMethod.POST, authRequest, new ParameterizedTypeReference<>() {}
        );

        List<Object> repositories = new ArrayList<>();

        if (query.contains("/")) {
            // Specific repository - get tags directly
            repositories.add(getRepositoryWithTags(query, username, password));
        } else {
            // Get list of repositories first
            String jwtToken = (String) Objects.requireNonNull(authResponse.getBody()).get("token");
            String userReposUrl = String.format("https://hub.docker.com/v2/repositories/%s/?page_size=100", query);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "JWT " + jwtToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                userReposUrl, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}
            );

            // For each repository, get its tags
            List<Object> repos = (List<Object>) Objects.requireNonNull(response.getBody()).get("results");
            for (Object repo : repos) {
                Map<String, Object> repoMap = (Map<String, Object>) repo;
                String repoName = (String) repoMap.get("name");
                String fullRepoName = query + "/" + repoName;
                repositories.add(getRepositoryWithTags(fullRepoName, username, password));
            }
        }

        return transformToSearchResponse(repositories, false, Collections.emptyMap());
    }

    private Map<String, Object> getRepositoryWithTags(String repository, String username, String password) {
        try {
            ResponseEntity<Map<String, Object>> tokenResponse = call(
                String.format("%s?service=%s&scope=repository:%s:pull", DOCKER_AUTH_URL, DOCKER_REGISTRY_SERVICE, repository),
                username, password, null
            );

            String registryToken = (String) Objects.requireNonNull(tokenResponse.getBody()).get("token");

            ResponseEntity<Map<String, Object>> tagsResponse = call(
                String.format("%s/%s/tags/list", DOCKER_REGISTRY_URL, repository),
                null, null, Map.of("Authorization", "Bearer " + registryToken)
            );

            Map<String, Object> result = new HashMap<>();
            result.put("name", repository);
            result.put("tags", Objects.requireNonNull(tagsResponse.getBody()).get("tags"));

            return result;
        } catch (HttpClientErrorException.NotFound e) {
            Map<String, Object> result = new HashMap<>();
            result.put("name", repository);
            result.put("tags", Collections.emptyList());
            return result;
        }
    }

    private DockerHubSearchResponse searchCustomRegistry(String url, String username, String password) {
        // Get list of repositories
        ResponseEntity<Map<String, Object>> response = call(
            url.contains("/v2/_catalog") ? url : StringUtils.join(url, "/v2/_catalog"),
            username, password, null
        );

        List<Object> repositories = new ArrayList<>();
        repositories.add(response.getBody());

        final String baseUrl = url.contains("/v2/_catalog") ? url.substring(0, url.indexOf("/v2/_catalog")) : url;

        // Get repository names
        Map<String, Object> repoMap = response.getBody();
        List<String> repoNames = (List<String>) repoMap.get("repositories");

        // Fetch tags for all repositories asynchronously
        List<CompletableFuture<Map<String, List<String>>>> futures = repoNames.stream()
            .map(repoName -> fetchRepositoryTags(baseUrl, repoName, username, password))
            .toList();

        // Wait for all tag fetches to complete
        Map<String, List<String>> repoTags = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    HashMap::new
                )))
            .join();

        // Transform response with the fetched tags
        return transformToSearchResponse(repositories, true, repoTags);
    }

    private DockerHubSearchResponse transformToSearchResponse(List<Object> repositories, boolean isCustomRegistry,
                                                              Map<String, List<String>> repoTags) {
        List<DockerHubSearchResponse.DockerHubSummary> summaries;

        if (isCustomRegistry && !repositories.isEmpty()) {
            Map<String, Object> repoMap = (Map<String, Object>) repositories.getFirst();
            List<String> repoNames = (List<String>) repoMap.get("repositories");

            summaries = repoNames.stream()
                .map(repoName -> {
                    DockerHubSearchResponse.DockerHubSummary summary = new DockerHubSearchResponse.DockerHubSummary();
                    summary.setName(repoName);
                    summary.setShortDescription("");
                    summary.setStarCount(0);
                    summary.setTags(repoTags.getOrDefault(repoName, new ArrayList<>()));
                    return summary;
                })
                .collect(Collectors.toList());
        } else {
            // Original Docker Hub transformation
            summaries = repositories.stream()
                .map(repo -> {
                    DockerHubSearchResponse.DockerHubSummary summary = new DockerHubSearchResponse.DockerHubSummary();
                    Map<String, Object> repoMap = (Map<String, Object>) repo;
                    summary.setName((String) repoMap.get("name"));
                    summary.setShortDescription((String) repoMap.get("description"));
                    summary.setStarCount(((Number) repoMap.getOrDefault("star_count", 0)).intValue());

                    if (repoMap.containsKey("tags")) {
                        summary.setTags((List<String>) repoMap.get("tags"));
                    }

                    return summary;
                })
                .collect(Collectors.toList());
        }

        DockerHubSearchResponse response = new DockerHubSearchResponse();
        response.setResults(summaries);
        return response;
    }

    @Async
    protected CompletableFuture<Map<String, List<String>>> fetchRepositoryTags(String baseUrl, String repoName, String username, String password) {
        try {
            ResponseEntity<Map<String, Object>> response = call(
                String.format("%s/v2/%s/tags/list", baseUrl, repoName),
                username, password, null
            );

            Map<String, Object> body = response.getBody();
            List<String> tags = body != null ? (List<String>) body.get("tags") : new ArrayList<>();
            return CompletableFuture.completedFuture(Map.of(repoName, tags));
        } catch (Exception e) {
            log.warn("Failed to fetch tags for repository {}: {}", repoName, e.getMessage());
            return CompletableFuture.completedFuture(Map.of(repoName, new ArrayList<>()));
        }
    }

    private ResponseEntity<Map<String, Object>> call(String url, String username, String password,
                                                     Map<String, String> headers) {
        return restTemplate.exchange(
            url,
            HttpMethod.GET,
            authenticatedRequest(username, password, headers),
            new ParameterizedTypeReference<>() {}
        );
    }

    private ResponseEntity<String> call(String url, Map<String, String> headers) {
        return restTemplate.exchange(
            url,
            HttpMethod.GET,
            authenticatedRequest(null, null, headers),
            String.class
        );
    }

    private ResponseEntity<Void> call(String url, String username, String password) {
        return restTemplate.exchange(
            url,
            HttpMethod.GET,
            authenticatedRequest(username, password, null),
            Void.class
        );
    }

    private HttpEntity<Void> authenticatedRequest(String username, String password, Map<String, String> headers) {
        HttpHeaders tokenHeaders = new HttpHeaders();
        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            tokenHeaders.set("Authorization", createBasicAuthHeader(username, password));
        }
        if (headers != null) {
            headers.forEach(tokenHeaders::set);
        }
        return new HttpEntity<>(tokenHeaders);
    }

    private String createBasicAuthHeader(String username, String password) {
        return StringUtils.join(
            "Basic ",
            Base64.getEncoder().encodeToString(
                StringUtils.join(
                    username, ":", password
                ).getBytes()
            )
        );
    }

    @Scheduled(fixedRate = ONE_DAY)
    @CacheEvict(value = {"publicRepos", "imageTags", "imageConfigs"}, allEntries = true)
    public void clearCache() {
        log.debug("DockerHub caches cleared.");
    }
}

