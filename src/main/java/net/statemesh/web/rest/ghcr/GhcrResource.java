package net.statemesh.web.rest.ghcr;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.statemesh.service.ghcr.GhcrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ghcr")
@RequiredArgsConstructor
@Tag(name = "GHCR", description = "GitHub Container Registry operations")
public class GhcrResource {
    private final GhcrService ghcrService;

    @Operation(summary = "Get image tags", description = "Get tags for a GHCR image")
    @ApiResponse(responseCode = "200", description = "Tags returned successfully")
    @GetMapping("/tags")
    public ResponseEntity<Map<String, Object>> getTags(
        @Parameter(description = "Image owner", example = "octocat") @RequestParam(name = "owner") String owner,
        @Parameter(description = "Image name", example = "hello-world") @RequestParam(name = "image") String imageName
    ) {
        return ResponseEntity.ok(ghcrService.getTags(owner, imageName));
    }

    @Operation(summary = "Get image config", description = "Get configuration (ports, volumes, env) for a GHCR image")
    @ApiResponse(responseCode = "200", description = "Image config returned successfully")
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getImageConfig(
        @Parameter(description = "Image owner", example = "octocat") @RequestParam(name = "owner") String owner,
        @Parameter(description = "Image name", example = "hello-world") @RequestParam(name = "image") String imageName,
        @Parameter(description = "Image tag", example = "latest") @RequestParam(name = "tag") String tag
    ) {
        return ResponseEntity.ok(ghcrService.getImageConfig(owner, imageName, tag));
    }
}
