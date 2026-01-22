package net.statemesh.service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DockerImageInfoDTO {
    private String digest;
    private String architecture;
    private Map<String, Object> config;
    private long size;
}
