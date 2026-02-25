package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.statemesh.domain.enumeration.ApiKeyProvider;
import net.statemesh.domain.enumeration.ApiKeyType;
import net.statemesh.security.SecurityUtils;
import net.statemesh.service.UserApiKeyService;
import net.statemesh.service.dto.UserApiKeyDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-api-keys")
@RequiredArgsConstructor
@Tag(name = "User API Key", description = "User API key management")
public class UserApiKeyResource {

    private final UserApiKeyService userApiKeyService;

    @Operation(summary = "Get current user's API keys by type")
    @ApiResponse(responseCode = "200", description = "List of API keys returned")
    @GetMapping
    public List<UserApiKeyDTO> getMyApiKeys(@RequestParam("type") ApiKeyType type) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        return userApiKeyService.getApiKeysForUser(login, type);
    }

    @Operation(summary = "Check if an API key exists for a provider")
    @ApiResponse(responseCode = "200", description = "Existence check result returned")
    @GetMapping("/{provider}/exists")
    public ResponseEntity<Boolean> hasApiKeyForProvider(
        @PathVariable("provider") String provider,
        @RequestParam("type") ApiKeyType type
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        ApiKeyProvider apiKeyProvider = ApiKeyProvider.fromValue(provider);
        return ResponseEntity.ok(userApiKeyService.hasApiKeyForProvider(login, apiKeyProvider, type));
    }

    @Operation(summary = "Save an API key")
    @ApiResponse(responseCode = "200", description = "API key saved")
    @PostMapping
    public UserApiKeyDTO saveApiKey(@Valid @RequestBody UserApiKeyDTO dto) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        return userApiKeyService.saveApiKey(login, dto);
    }

    @Operation(summary = "Delete an API key by provider")
    @ApiResponse(responseCode = "204", description = "API key deleted")
    @DeleteMapping("/{provider}")
    public ResponseEntity<Void> deleteApiKey(
        @PathVariable("provider") String provider,
        @RequestParam("type") ApiKeyType type
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        ApiKeyProvider apiKeyProvider = ApiKeyProvider.fromValue(provider);
        userApiKeyService.deleteApiKey(login, apiKeyProvider, type);
        return ResponseEntity.noContent().build();
    }
}
