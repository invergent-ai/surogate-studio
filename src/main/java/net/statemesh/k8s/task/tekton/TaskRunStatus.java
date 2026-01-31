package net.statemesh.k8s.task.tekton;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.statemesh.domain.enumeration.TaskRunProvisioningStatus;

import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
public class TaskRunStatus {
    private String taskId;
    private String stage;
    private TaskRunProvisioningStatus provisioningStatus;
    private String message;
    private Instant startTime;
    private Instant completionTime;
    private String podName;
    private String container;
    private String progress;
}
