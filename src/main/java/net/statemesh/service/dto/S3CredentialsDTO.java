package net.statemesh.service.dto;

import lombok.Data;

@Data
public class S3CredentialsDTO {
    private String accessKey;
    private String secretKey;
    private String bucketUrl;
    private String bucketName;
    private String region;
    private String applicationId;
    private String volumeName;
}
