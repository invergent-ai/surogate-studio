package net.statemesh.web.rest.ghcr;

import lombok.RequiredArgsConstructor;
import net.statemesh.service.ghcr.GhcrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ghcr")
@RequiredArgsConstructor
public class GhcrResource {
    private final GhcrService ghcrService;

    @GetMapping("/tags")
    public ResponseEntity<Map<String, Object>> getTags(
        @RequestParam(name = "owner") String owner,
        @RequestParam(name = "image") String imageName
    ) {
        return ResponseEntity.ok(ghcrService.getTags(owner, imageName));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getImageConfig(
        @RequestParam(name = "owner") String owner,
        @RequestParam(name = "image") String imageName,
        @RequestParam(name = "tag") String tag
    ) {
        return ResponseEntity.ok(ghcrService.getImageConfig(owner, imageName, tag));
    }
}
