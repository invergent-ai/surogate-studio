package net.statemesh.service.dto;

import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LineDTO implements Serializable {
    private String applicationId;
    private String podName;
    private String containerId;
    private String vmId;
    private String jobId;
    private String message;
}
