package net.statemesh.service.dto;

import lombok.Data;

@Data
public class ExternalDeployDTO {
    // Common
    private String appName;
    private String port;
    // Deploy from GIT
    private String referrer;
    private String subPath;
    // Install from Docker image
    private String image;
    private String tag;
}
