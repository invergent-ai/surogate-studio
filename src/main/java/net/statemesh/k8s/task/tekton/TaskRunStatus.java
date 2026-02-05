package net.statemesh.k8s.task.tekton;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.statemesh.domain.enumeration.RayJobProvisioningStatus;
import net.statemesh.domain.enumeration.TaskRunProvisioningStatus;
import net.statemesh.k8s.task.ray.RayJobStatus;

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

    public RayJobStatus toRayJobStatus() {
        return RayJobStatus.builder()
            .rayJobId(taskId)
            .submissionId(taskId)
            .stage(stage)
            .message(message)
            .startTime(startTime)
            .completionTime(completionTime)
            .podName(podName)
            .container(container)
            .progress(progress)
            .provisioningStatus(provisioningStatus != null ? toRayJobProvisioningStatus(provisioningStatus) : null)
            .build();
    }

    private RayJobProvisioningStatus toRayJobProvisioningStatus(TaskRunProvisioningStatus taskRunProvisioningStatus) {
        return switch (taskRunProvisioningStatus) {
            case CREATED -> RayJobProvisioningStatus.CREATED;
            case DEPLOYING -> RayJobProvisioningStatus.DEPLOYING;
            case DEPLOYED -> RayJobProvisioningStatus.DEPLOYED;
            case ERROR -> RayJobProvisioningStatus.ERROR;
            case COMPLETED -> RayJobProvisioningStatus.COMPLETED;
            case CANCELLED -> RayJobProvisioningStatus.CANCELLED;
        };
    }
}
