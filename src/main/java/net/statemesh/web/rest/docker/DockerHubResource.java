package net.statemesh.web.rest.docker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.statemesh.service.docker.DockerHubService;
import net.statemesh.service.dto.DockerHubSearchResponse;
import net.statemesh.service.dto.DockerHubTagResponse;
import net.statemesh.service.dto.RegistryValidationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Docker Hub", description = "Docker Hub and private registry operations")
public class DockerHubResource {
    private final DockerHubService dockerHubService;

    @Operation(summary = "Search Docker Hub images", description = "Search for public images on Docker Hub")
    @ApiResponse(responseCode = "200", description = "Search results returned successfully")
    @GetMapping("/docker-hub/search")
    public ResponseEntity<DockerHubSearchResponse> searchImages(
        @Parameter(description = "Search query") @RequestParam(required = false, name = "q") String q) {
        return ResponseEntity.ok(dockerHubService.searchImages(q));
    }

    @Operation(summary = "Get image tags", description = "Get tags for a namespaced Docker Hub image")
    @ApiResponse(responseCode = "200", description = "Tags returned successfully")
    @GetMapping("/docker-hub/tags/{namespace}/{repository}")
    public ResponseEntity<DockerHubTagResponse> getImageTags(
        @Parameter(description = "Image namespace", example = "library") @PathVariable(name = "namespace") String namespace,
        @Parameter(description = "Repository name", example = "nginx") @PathVariable(name = "repository") String repository
    ) {
        String imageName = namespace + "/" + repository;
        return ResponseEntity.ok(dockerHubService.getPublicImageTags(imageName));
    }

    @Operation(summary = "Get official image tags", description = "Get tags for an official Docker Hub image")
    @ApiResponse(responseCode = "200", description = "Tags returned successfully")
    @GetMapping("/docker-hub/tags/{imageName}")
    public ResponseEntity<DockerHubTagResponse> getOfficialImageTags(
        @Parameter(description = "Official image name", example = "nginx") @PathVariable(name = "imageName") String imageName
    ) {
        return ResponseEntity.ok(dockerHubService.getPublicImageTags(imageName));
    }

    @Operation(summary = "Get image config", description = "Get configuration (ports, volumes, env) for a Docker image")
    @ApiResponse(responseCode = "200", description = "Image config returned successfully")
    @GetMapping({
        "/docker-hub/config/{imageName}/{tag}",
        "/docker-hub/config/{namespace}/{imageName}/{tag}"
    })
    public ResponseEntity<Map<String, Object>> getImageConfig(
        @Parameter(description = "Image namespace (optional)") @PathVariable(required = false, name = "namespace") String namespace,
        @Parameter(description = "Image name", example = "nginx") @PathVariable(name = "imageName") String imageName,
        @Parameter(description = "Image tag", example = "latest") @PathVariable(name = "tag") String tag
    ) {
        String fullImageName = namespace == null ?
            "library/" + imageName :
            namespace + "/" + imageName;
        return ResponseEntity.ok(dockerHubService.getImageConfig(fullImageName, tag));
    }

    @Operation(summary = "Validate registry credentials", description = "Validate credentials for a private Docker registry")
    @ApiResponse(responseCode = "200", description = "Credentials are valid")
    @ApiResponse(responseCode = "400", description = "Invalid credentials")
    @PostMapping("/docker-hub/validate/registry")
    public ResponseEntity<Void> validateRegistryCredentials(@RequestBody RegistryValidationRequest request) {
        try {
            dockerHubService.validateCredentials(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Search private registry images", description = "Search for images in a private Docker registry")
    @ApiResponse(responseCode = "200", description = "Search results returned successfully")
    @GetMapping("/docker-hub/registry/search")
    public ResponseEntity<DockerHubSearchResponse> searchRegistryImages(
        @Parameter(description = "Registry URL", required = true) @RequestParam(name = "url") String url,
        @Parameter(description = "Registry username") @RequestParam(required = false, name = "username") String username,
        @Parameter(description = "Registry password") @RequestParam(required = false, name = "password") String password
    ) {
        return ResponseEntity.ok(dockerHubService.searchImagesPrivateRepo(url, username, password));
    }

    @Operation(summary = "Get private registry image config", description = "Get ports and volumes for a private registry image")
    @ApiResponse(responseCode = "200", description = "Image config returned successfully")
    @PostMapping("docker-hub/docker-registry/ports-and-volumes")
    public ResponseEntity<Map<String, Object>> getRegistryImageConfig(
        @RequestBody Map<String, String> request
    ) {
        String namespace = request.get("namespace");
        String imageName = request.get("imageName");
        String tag = request.get("tag");
        String registryUrl = request.get("registryUrl");
        String registryUser = request.get("registryUser");
        String registryPassword = request.get("registryPassword");

        if (namespace.equals(imageName)) {
            return ResponseEntity.ok(
                dockerHubService.getPrivateRegistryImageDigestConfig(
                    registryUrl, imageName, tag, registryUser, registryPassword
                )
            );
        }

        return ResponseEntity.ok(
            dockerHubService.getPrivateRegistryImageConfig(
                registryUrl, namespace, imageName, tag, registryUser, registryPassword
            )
        );
    }

    @Operation(summary = "Get private registry image tags", description = "Get tags for an image in a private registry")
    @ApiResponse(responseCode = "200", description = "Tags returned successfully")
    @GetMapping("/docker-hub/registry/tags")
    public ResponseEntity<Map<String, Object>> getRegistryImageTags(
        @Parameter(description = "Registry URL", required = true) @RequestParam(name = "url") String url,
        @Parameter(description = "Image name", required = true) @RequestParam(name = "image") String image,
        @Parameter(description = "Registry username") @RequestParam(required = false, name = "username") String username,
        @Parameter(description = "Registry password") @RequestParam(required = false, name = "password") String password
    ) {
        return ResponseEntity.ok(dockerHubService.getPrivateRegistryImageTags(url, username, password));
    }
}
