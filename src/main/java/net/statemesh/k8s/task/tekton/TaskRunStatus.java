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
    String taskId;
    String stage;
    TaskRunProvisioningStatus provisioningStatus;
    String message;
    Instant startTime;
    Instant completionTime;
    String podName;
    String container;
    String progress;
}
