package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreCLIResponseDTO {
    @JsonProperty("browser_url")
    private String browserUrl;

    @JsonProperty("token")
    private String token;
}
