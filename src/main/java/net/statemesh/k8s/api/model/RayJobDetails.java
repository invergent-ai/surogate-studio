package net.statemesh.k8s.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RayJobDetails {
    private String type;
    @JsonProperty("job_id")
    private String jobId;
    @JsonProperty("submission_id")
    private String submissionId;
    private String status;
    private String entrypoint;
    private String message;
    @JsonProperty("error_type")
    private String errorType;
    @JsonProperty("start_time")
    private Long startTime;
    @JsonProperty("end_time")
    private Long endTime;
    @JsonProperty("driver_agent_http_address")
    private String driverAgentHttpAddress;
    @JsonProperty("driver_node_id")
    private String driverAgentNodeId;
    @JsonProperty("driver_exit_code")
    private String driverExitCode;
}
