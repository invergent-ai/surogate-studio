package net.statemesh.k8s.task.ray;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.crd.rayjob.models.V1RayJob;
import net.statemesh.k8s.task.BaseTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.RayJobDTO;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class ReadRayJobStatusTask extends BaseTask<List<RayJobStatus>> {
    private final Set<RayJobDTO> rayJobs;

    public ReadRayJobStatusTask(ApiStub apiStub,
                                TaskConfig taskConfig,
                                String namespace,
                                Set<RayJobDTO> rayJobs) {
        super(apiStub, taskConfig, namespace);
        this.rayJobs = rayJobs;
    }

    @Override
    public CompletableFuture<TaskResult<List<RayJobStatus>>> call() {
        try {
            var requestedJobNames = this.rayJobs.stream()
                .map(RayJobDTO::getInternalName)
                .collect(Collectors.toSet());
            var rayJobList = getApiStub().getRayJob().list(getNamespace())
                .throwsApiException()
                .getObject();
            var relevantJobs = rayJobList.getItems().stream()
                .filter(t -> requestedJobNames.contains(t.getMetadata().getName()))
                .collect(Collectors.toSet());
            List<RayJobStatus> statuses = relevantJobs.stream()
                .map(this::toRayJobStatus)
                .toList();
            return CompletableFuture.completedFuture(
                TaskResult.<List<RayJobStatus>>builder()
                    .success(Boolean.TRUE)
                    .value(statuses)
                    .build()
            );
        } catch (ApiException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private RayJobStatus toRayJobStatus(V1RayJob rayJob) {
        var result = RayJobStatus.builder()
            .rayJobId(
                this.rayJobs.stream()
                    .filter(j -> rayJob.getMetadata().getName().equals(j.getInternalName()))
                .findFirst()
                .map(RayJobDTO::getId)
                .orElse(null)
            );

        if (rayJob.getStatus() != null) {
            result.startTime(rayJob.getStatus().getRayJobInfo() != null &&
                    rayJob.getStatus().getRayJobInfo().getStartTime() != null ?
                    rayJob.getStatus().getRayJobInfo().getStartTime().toInstant() : null)
                .completionTime(rayJob.getStatus().getRayJobInfo() != null &&
                    rayJob.getStatus().getRayJobInfo().getEndTime() != null ?
                    rayJob.getStatus().getRayJobInfo().getEndTime().toInstant() : null)
                .stage(1 == rayJob.getStatus().getFailed() ? "FAILED" : rayJob.getStatus().getJobStatus())
                .message(rayJob.getStatus().getMessage())
                .submissionId(rayJob.getStatus().getJobId());
        }

        return result.build();
    }
}
