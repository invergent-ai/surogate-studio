package net.statemesh.service.dto.mixin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public abstract class SkyConfigMixin {
    @JsonProperty("workdir")
    private String workDir;

    @JsonProperty("num_nodes")
    private String numNodes;

    @JsonProperty("file_mounts")
    private Map<String, String> fileMounts;
}
