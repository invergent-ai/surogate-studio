package net.statemesh.service.k8s.status;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.RayJobProvisioningStatus;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.task.ray.RayJobStatus;
import net.statemesh.k8s.util.ApiUtils;
import net.statemesh.service.*;
import net.statemesh.service.dto.RayJobDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static net.statemesh.config.K8Timeouts.COPY_FILE_TIMEOUT_SECONDS;
import static net.statemesh.k8s.util.K8SConstants.RAY_JOB_SUBMITTER_CONTAINER_NAME;

@Service
@Slf4j
public class RayJobStatusService extends PollingEventStreamService {
    public RayJobStatusService(
        ApplicationService applicationService,
        ContainerService containerService,
        TaskRunService taskRunService,
        RayJobService rayJobService,
        KubernetesController kubernetesController,
        ApplicationProperties applicationProperties,
        @Qualifier("statusScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(applicationService, containerService, taskRunService, rayJobService, kubernetesController, applicationProperties, taskScheduler);
    }

    @Override
    protected void onTick(String... emId) throws InterruptedException, TimeoutException {
        var rayJobs = rayJobService.findRayJobsById(emId).stream()
            .filter(job -> !RayJobProvisioningStatus.ERROR.equals(job.getProvisioningStatus())
                && !RayJobProvisioningStatus.COMPLETED.equals(job.getProvisioningStatus()))
            .collect(Collectors.toSet());
        if (rayJobs.isEmpty()) {
            return;
        }

        try {
            var result = getRayJobStatuses(rayJobs);
            if (result == null) {
                return;
            }

            var statuses = updateProvisioningStatusFromStatus(rayJobs, result.getValue());
            try {
                sendEvent("rayjobstatus", statuses, emId);
            } catch (Exception e) {
                log.error("Failed to send status update to SSE for ray job {}", emId, e);
            }
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ApiException)) {
                throw new RuntimeException(e);
            } else {
                log.trace("Error reading ray job statuses with message {}", e.getMessage());
            }
        }
    }

    private List<RayJobStatus> updateProvisioningStatusFromStatus(Set<RayJobDTO> rayJobs, List<RayJobStatus> resourceStatuses) {
        resourceStatuses.forEach(status -> {
            var job = rayJobs.stream()
                .filter(j -> j.getId().equals(status.getRayJobId()))
                .findFirst()
                .orElse(null);

            if (job != null) {
                if ("RUNNING".equals(status.getStage()) && status.getStartTime() != null) {
                    status.setProvisioningStatus(RayJobProvisioningStatus.DEPLOYED);
                    rayJobService.updateProvisioningStatus(job.getId(), RayJobProvisioningStatus.DEPLOYED);
                    rayJobService.updateStartTime(job.getId(), status.getStartTime());
                    job.setProvisioningStatus(RayJobProvisioningStatus.DEPLOYED);

                    try {
                        String podName = ApiUtils.rayJobPodName(
                            kubernetesController.getApi(job.getProject().getCluster()).getCoreV1Api(),
                            job.getDeployedNamespace(),
                            job.getInternalName()
                        );
                        rayJobService.updatePodAndContainer(job.getId(), podName, RAY_JOB_SUBMITTER_CONTAINER_NAME);
                        rayJobService.updateSubmissionId(job.getId(), status.getSubmissionId());
                        job.setPodName(podName);
                        job.setContainer(RAY_JOB_SUBMITTER_CONTAINER_NAME);
                        job.setSubmissionId(status.getSubmissionId());
                        status.setPodName(podName);
                        status.setContainer(RAY_JOB_SUBMITTER_CONTAINER_NAME);
                        status.setSubmissionId(status.getSubmissionId());
                    } catch (ApiException e) {
                        log.error("Could not determine ray job pod for job {}", job.getInternalName(), e);
                    }
                } else if (("SUCCEEDED".equals(status.getStage()) || "FAILED".equals(status.getStage()))) {
                    status.setProvisioningStatus(RayJobProvisioningStatus.COMPLETED);
                    rayJobService.updateProvisioningStatus(job.getId(), RayJobProvisioningStatus.COMPLETED);
                    rayJobService.updateCompletedStatus(job.getId(), status.getStage());
                    if (status.getCompletionTime() != null) {
                        rayJobService.updateEndTime(job.getId(), status.getCompletionTime());
                    }
                    job.setProvisioningStatus(RayJobProvisioningStatus.COMPLETED);
                    job.setCompletedStatus(status.getStage());
                } else if ("RayJobCancelled".equals(status.getStage())) {
                    status.setProvisioningStatus(RayJobProvisioningStatus.CANCELLED);
                    rayJobService.updateProvisioningStatus(job.getId(), RayJobProvisioningStatus.CANCELLED);
                    job.setProvisioningStatus(RayJobProvisioningStatus.CANCELLED);
                }
            }
        });
        return resourceStatuses;
    }

    private TaskResult<List<RayJobStatus>> getRayJobStatuses(Set<RayJobDTO> rayJobs)
        throws ExecutionException, InterruptedException, TimeoutException {
        var sample = rayJobs.iterator().next();
        return this.kubernetesController.readRayJobStatuses(
            Objects.isNull(sample.getDeployedNamespace()) ? sample.getInternalName() : sample.getDeployedNamespace(),
            sample.getProject().getCluster(),
            rayJobs
        ).get(COPY_FILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
