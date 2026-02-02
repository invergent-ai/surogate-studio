package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.statemesh.domain.enumeration.ApiKeyProvider;
import net.statemesh.domain.enumeration.ApiKeyType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserApiKeyDTO {

    private String id;

    @NotNull
    private ApiKeyType type;

    @NotNull
    private ApiKeyProvider provider;

    private String apiKey;

    private String maskedApiKey;
}
