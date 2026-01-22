package net.statemesh.k8s.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AimRun {
    @JsonProperty("run_id")
    private String runId;
    private String name;
    private Boolean archived;
    @JsonProperty("creation_time")
    private Long creationTime;
    @JsonProperty("end_time")
    private Long endTime;
}
