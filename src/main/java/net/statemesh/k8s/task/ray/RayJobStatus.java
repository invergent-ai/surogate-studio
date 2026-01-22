package net.statemesh.k8s.task.ray;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.statemesh.domain.enumeration.RayJobProvisioningStatus;

import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
public class RayJobStatus {
    String rayJobId;
    String stage;
    RayJobProvisioningStatus provisioningStatus;
    String message;
    Instant startTime;
    Instant completionTime;
    String submissionId;
    String podName;
    String container;
    String progress;
}
