package net.statemesh.service.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class S3ValidationRequest {
    @NotNull
    private String accessKey;

    @NotNull
    private String secretKey;

    @NotNull
    private String bucketUrl;
}
