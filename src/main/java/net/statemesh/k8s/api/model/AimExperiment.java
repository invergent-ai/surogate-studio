package net.statemesh.k8s.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AimExperiment {
    private String id;
    private String name;
    private String description;
    @JsonProperty("run_count")
    private Integer runCount;
    private Boolean archived;
    @JsonProperty("creation_time")
    private Long creationTime;
    private List<AimRun> runs;
}
