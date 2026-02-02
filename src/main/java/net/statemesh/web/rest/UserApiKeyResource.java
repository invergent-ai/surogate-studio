package net.statemesh.web.rest;

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
public class UserApiKeyResource {

    private final UserApiKeyService userApiKeyService;

    @GetMapping
    public List<UserApiKeyDTO> getMyApiKeys(@RequestParam("type") ApiKeyType type) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        return userApiKeyService.getApiKeysForUser(login, type);
    }

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

    @PostMapping
    public UserApiKeyDTO saveApiKey(@Valid @RequestBody UserApiKeyDTO dto) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        return userApiKeyService.saveApiKey(login, dto);
    }

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
