package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import net.statemesh.domain.UserApiKey;
import net.statemesh.domain.enumeration.ApiKeyProvider;
import net.statemesh.domain.enumeration.ApiKeyType;
import net.statemesh.repository.UserApiKeyRepository;
import net.statemesh.repository.UserRepository;
import net.statemesh.security.ApiKeyEncryption;
import net.statemesh.service.dto.UserApiKeyDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserApiKeyService {

    private final UserApiKeyRepository userApiKeyRepository;
    private final UserRepository userRepository;
    private final ApiKeyEncryption apiKeyEncryption;

    public List<UserApiKeyDTO> getApiKeysForUser(String login, ApiKeyType type) {
        return userApiKeyRepository.findByUserLoginAndType(login, type).stream()
            .map(this::toMaskedDto)
            .toList();
    }

    public boolean hasApiKeyForProvider(String login, ApiKeyProvider provider, ApiKeyType type) {
        return userApiKeyRepository.findByUserLoginAndProviderAndType(login, provider, type).isPresent();
    }

    public Optional<String> getDecryptedApiKey(String login, ApiKeyProvider provider, ApiKeyType type) {
        return userApiKeyRepository.findByUserLoginAndProviderAndType(login, provider, type)
            .map(entity -> apiKeyEncryption.decrypt(entity.getApiKey()));
    }

    public UserApiKeyDTO saveApiKey(String login, UserApiKeyDTO dto) {
        var user = userRepository.findOneByLoginIgnoreCase(login)
            .orElseThrow(() -> new RuntimeException("User not found"));

        var existing = userApiKeyRepository.findByUserLoginAndProviderAndType(login, dto.getProvider(), dto.getType());

        String encryptedApiKey = apiKeyEncryption.encrypt(dto.getApiKey());

        UserApiKey entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setApiKey(encryptedApiKey);
        } else {
            entity = UserApiKey.builder()
                .user(user)
                .type(dto.getType())
                .provider(dto.getProvider())
                .apiKey(encryptedApiKey)
                .build();
        }

        return toMaskedDto(userApiKeyRepository.save(entity));
    }

    public void deleteApiKey(String login, ApiKeyProvider provider, ApiKeyType type) {
        userApiKeyRepository.deleteByUserLoginAndProviderAndType(login, provider, type);
    }

    private UserApiKeyDTO toMaskedDto(UserApiKey entity) {
        String decryptedKey = apiKeyEncryption.decrypt(entity.getApiKey());
        String masked = maskApiKey(decryptedKey);
        return UserApiKeyDTO.builder()
            .id(entity.getId())
            .type(entity.getType())
            .provider(entity.getProvider())
            .maskedApiKey(masked)
            .build();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
