package net.statemesh.service.k8s.status;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.TaskRunProvisioningStatus;
import net.statemesh.domain.enumeration.TaskRunType;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.task.tekton.TaskRunStatus;
import net.statemesh.service.*;
import net.statemesh.service.dto.TaskRunDTO;
import net.statemesh.service.lakefs.LakeFsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.statemesh.config.K8Timeouts.COPY_FILE_TIMEOUT_SECONDS;
import static net.statemesh.config.K8Timeouts.READ_LOGS_TIMEOUT_SECONDS;

@Service
@Slf4j
public class TaskRunStatusService extends PollingEventStreamService {
    private final LakeFsService lakeFsService;
    private final AsyncTaskExecutor smTaskExecutor;

    private static final Pattern PROGRESS_PATTERN =
        Pattern.compile("\\bPROGRESS:\\s*(\\S.*)$");

    public TaskRunStatusService(
        ApplicationService applicationService,
        ContainerService containerService,
        TaskRunService taskRunService,
        RayJobService rayJobService,
        KubernetesController kubernetesController,
        ApplicationProperties applicationProperties,
        LakeFsService lakeFsService,
        @Qualifier("statusScheduler") ThreadPoolTaskScheduler taskScheduler, AsyncTaskExecutor smTaskExecutor) {
        super(applicationService, containerService, taskRunService, rayJobService, kubernetesController, applicationProperties, taskScheduler);
        this.lakeFsService = lakeFsService;
        this.smTaskExecutor = smTaskExecutor;
    }

    @Override
    protected void onTick(String... emId) throws InterruptedException, TimeoutException {
        // we only want tasks in progress, so skip ERROR and DEPLOYED
        var taskRuns = taskRunService.findTaskRunsById(emId).stream()
            .filter(tr -> !TaskRunProvisioningStatus.ERROR.equals(tr.getProvisioningStatus())
                && !TaskRunProvisioningStatus.COMPLETED.equals(tr.getProvisioningStatus()))
            .collect(Collectors.toSet());

        if (taskRuns.isEmpty()) {
            return;
        }

        try {
            var result = getTaskRunStatuses(taskRuns);
            if (result == null) {
                return; // error already logged
            }

            var statuses = updateProvisioningStatusFromStatus(taskRuns, result.getValue());

            // send an event as soon as possible
            try {
                sendEvent("taskstatus", statuses, emId);
            } catch (Exception e) {
                log.error("Failed to send status update to SSE for task run {}", emId, e);
            }

            updateProgressForRunningTasks(taskRuns, statuses)
                .orTimeout(READ_LOGS_TIMEOUT_SECONDS * statuses.size(), TimeUnit.SECONDS)
                .thenRun(() -> {
                    try {
                        // send another one with logs
                        sendEvent("taskstatus", statuses, emId);
                    } catch (Exception e) {
                        log.error("Failed to send status update to SSE for task run {}", emId, e);
                    }
                });
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ApiException)) {
                throw new RuntimeException(e);
            } else {
                log.trace("Error reading taskRun statuses with message {}", e.getMessage());
            }
        }
    }

    private List<TaskRunStatus> updateProvisioningStatusFromStatus(Set<TaskRunDTO> taskRuns, List<TaskRunStatus> resourceStatuses) {
        resourceStatuses.forEach(status -> {
            var taskRun = taskRuns.stream()
                .filter(tr -> tr.getId().equals(status.getTaskId()))
                .findFirst()
                .orElse(null);

            if (taskRun != null) {
                if ("Succeeded".equals(status.getStage()) && status.getCompletionTime() != null) {
                    status.setProvisioningStatus(TaskRunProvisioningStatus.COMPLETED);
                    taskRunService.updateProvisioningStatus(taskRun.getId(), TaskRunProvisioningStatus.COMPLETED);
                    taskRunService.updateCompletedStatus(taskRun.getId(), status.getStage());
                    taskRunService.updateEndTime(taskRun.getId(), status.getCompletionTime());
                    taskRun.setProvisioningStatus(TaskRunProvisioningStatus.COMPLETED);
                    taskRun.setCompletedStatus(status.getStage());
                    log.debug("Set completed");
                } else if ("Failed".equals(status.getStage())) {
                    status.setProvisioningStatus(TaskRunProvisioningStatus.ERROR);
                    taskRunService.updateProvisioningStatus(taskRun.getId(), TaskRunProvisioningStatus.ERROR);
                    taskRunService.updateCompletedStatus(taskRun.getId(), status.getStage());
                    taskRun.setProvisioningStatus(TaskRunProvisioningStatus.ERROR);
                    taskRun.setCompletedStatus(status.getStage());
                    cleanupOnError(taskRun);
                } else if ("TaskRunCancelled".equals(status.getStage())) {
                    status.setProvisioningStatus(TaskRunProvisioningStatus.CANCELLED);
                    taskRunService.updateProvisioningStatus(taskRun.getId(), TaskRunProvisioningStatus.CANCELLED);
                    taskRun.setProvisioningStatus(TaskRunProvisioningStatus.CANCELLED);
                } else if (TaskRunProvisioningStatus.DEPLOYING.equals(taskRun.getProvisioningStatus()) && status.getStartTime() != null) {
                    status.setProvisioningStatus(TaskRunProvisioningStatus.DEPLOYED);
                    taskRunService.updateProvisioningStatus(taskRun.getId(), TaskRunProvisioningStatus.DEPLOYED);
                    taskRunService.updateStartTime(taskRun.getId(), status.getStartTime());
                    taskRunService.updatePodAndContainer(taskRun.getId(), status.getPodName(), status.getContainer());
                    taskRun.setPodName(status.getPodName());
                    taskRun.setContainer(status.getContainer());
                    taskRun.setProvisioningStatus(TaskRunProvisioningStatus.DEPLOYED);
                }
            }
        });
        return resourceStatuses;
    }

    private CompletableFuture<Void> updateProgressForRunningTasks(Set<TaskRunDTO> taskRuns, List<TaskRunStatus> resourceStatuses) {
        var tasksToCollectLogsFrom = new HashSet<TaskRunDTO>();
        for (TaskRunStatus status : resourceStatuses) {
            if ("Running".equals(status.getStage())) {
                var optTaskRun = taskRuns.stream().filter(tr -> tr.getId().equals(status.getTaskId())).findFirst();
                optTaskRun.ifPresent(tasksToCollectLogsFrom::add);
            }
        }

        var futures = new ArrayList<CompletableFuture<Void>>();
        for (TaskRunDTO taskRun : tasksToCollectLogsFrom) {
            futures.add(smTaskExecutor.submitCompletable(() -> {
                try {
                    var result = kubernetesController.readLogs(
                        Objects.isNull(taskRun.getDeployedNamespace()) ? taskRun.getInternalName() : taskRun.getDeployedNamespace(),
                        taskRun.getProject().getCluster(),
                        null,
                        10,
                        null,
                        taskRun.getPodName(),
                        taskRun.getContainer()
                    ).get(READ_LOGS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    if (result.isSuccess()) {
                        try (var reader = new BufferedReader(new InputStreamReader(result.getValue()))) {
                            // parse the logs in reverse order to find a PROGRESS message
                            for (var line : reader.lines().toList().reversed()) {
                                var progress = extractProgress(line);
                                progress.ifPresent(resourceId -> resourceStatuses.stream()
                                    .filter(s -> s.getTaskId().equals(taskRun.getId()))
                                    .findFirst()
                                    .ifPresent(status -> status.setProgress(resourceId)));
                            }
                        }
                    }
                } catch (Exception e) {
                    // do nothing, this is a best-effort to get the task progress from logs
                }
            }));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private Optional<String> extractProgress(String line) {
        var m = PROGRESS_PATTERN.matcher(line);
        if (m.find()) {
            return Optional.of(m.group(1).trim());
        }
        return Optional.empty();
    }

    public TaskResult<List<TaskRunStatus>> getTaskRunStatuses(Set<TaskRunDTO> taskRuns) throws ExecutionException, InterruptedException, TimeoutException {
        var sampleTaskRun = taskRuns.iterator().next();
        return this.kubernetesController.readTaskRunStatuses(
            Objects.isNull(sampleTaskRun.getDeployedNamespace()) ? sampleTaskRun.getInternalName() : sampleTaskRun.getDeployedNamespace(),
            sampleTaskRun.getProject().getCluster(),
            taskRuns
        ).get(COPY_FILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void cleanupOnError(TaskRunDTO taskRunDTO) {
        try {
            if (TaskRunType.IMPORT_HF_MODEL.equals(taskRunDTO.getType()) || TaskRunType.IMPORT_HF_DATASET.equals(taskRunDTO.getType())) {
                // delete lakefs repository
                taskRunDTO.getParams().stream()
                    .filter(p -> "lakefs-repo".equals(p.getKey()))
                    .findFirst()
                    .ifPresent(param -> {
                        lakeFsService.deleteRepository(param.getValue());
                    });
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup TaskRun {}", taskRunDTO.getName(), e);
        }
    }
}
