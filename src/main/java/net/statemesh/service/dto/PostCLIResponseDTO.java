package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostCLIResponseDTO {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("error")
    private String error;
}
