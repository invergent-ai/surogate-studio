package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistryValidationRequest {
    @NotNull
    private String url;
    private String username;
    private String password;
    private String applicationId;
    private String imageName;
}
