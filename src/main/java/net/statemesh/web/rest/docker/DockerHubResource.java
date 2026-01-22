package net.statemesh.web.rest.docker;

import lombok.RequiredArgsConstructor;
import net.statemesh.service.docker.DockerHubService;
import net.statemesh.service.dto.DockerHubSearchResponse;
import net.statemesh.service.dto.DockerHubTagResponse;
import net.statemesh.service.dto.RegistryValidationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DockerHubResource {
    private final DockerHubService dockerHubService;

    @GetMapping("/docker-hub/search")
    public ResponseEntity<DockerHubSearchResponse> searchImages(@RequestParam(required = false, name = "q") String q) {
        return ResponseEntity.ok(dockerHubService.searchImages(q));
    }

    @GetMapping("/docker-hub/tags/{namespace}/{repository}")
    public ResponseEntity<DockerHubTagResponse> getImageTags(
        @PathVariable(name = "namespace") String namespace,
        @PathVariable(name = "repository") String repository
    ) {
        String imageName = namespace + "/" + repository;
        return ResponseEntity.ok(dockerHubService.getPublicImageTags(imageName));
    }

    @GetMapping("/docker-hub/tags/{imageName}")
    public ResponseEntity<DockerHubTagResponse> getOfficialImageTags(
        @PathVariable(name = "imageName") String imageName
    ) {
        return ResponseEntity.ok(dockerHubService.getPublicImageTags(imageName));
    }

    @GetMapping({
        "/docker-hub/config/{imageName}/{tag}",
        "/docker-hub/config/{namespace}/{imageName}/{tag}"
    })
    public ResponseEntity<Map<String, Object>> getImageConfig(
        @PathVariable(required = false, name = "namespace") String namespace,
        @PathVariable(name = "imageName") String imageName,
        @PathVariable(name = "tag") String tag
    ) {
        String fullImageName = namespace == null ?
            "library/" + imageName :
            namespace + "/" + imageName;

        return ResponseEntity.ok(dockerHubService.getImageConfig(fullImageName, tag));
    }

    @PostMapping("/docker-hub/validate/registry")
    public ResponseEntity<Void> validateRegistryCredentials(@RequestBody RegistryValidationRequest request) {
        try {
            dockerHubService.validateCredentials(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/docker-hub/registry/search")
    public ResponseEntity<DockerHubSearchResponse> searchRegistryImages(
        @RequestParam(name = "url") String url,
        @RequestParam(required = false, name = "username") String username,
        @RequestParam(required = false, name = "password") String password
    ) {
        return ResponseEntity.ok(dockerHubService.searchImagesPrivateRepo(url, username, password));
    }


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
                    registryUrl,
                    imageName,
                    tag,
                    registryUser,
                    registryPassword
                )
            );
        }

        return ResponseEntity.ok(
            dockerHubService.getPrivateRegistryImageConfig(
                registryUrl,
                namespace,
                imageName,
                tag,
                registryUser,
                registryPassword
            )
        );
    }
    @GetMapping("/docker-hub/registry/tags")
    public ResponseEntity<Map<String, Object>> getRegistryImageTags(
        @RequestParam(name = "url") String url,
        @RequestParam(name = "image") String image,
        @RequestParam(required = false, name = "username") String username,
        @RequestParam(required = false, name = "password") String password
    ) {
        return ResponseEntity.ok(dockerHubService.getPrivateRegistryImageTags(url, username, password));
    }
}
