package net.statemesh.service.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Data
public class DockerHubTagResponse {
    private List<DockerHubTag> results;
    private Map<String, Object> exposedPorts;
    @Getter
    @Setter
    public static class DockerHubTag {
        private String name;
        private String lastUpdated;
        private List<DockerImageInfoDTO> images;
    }
}

