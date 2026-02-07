package net.statemesh.k8s.task.tekton;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.crd.tekton.models.V1TaskRun;
import net.statemesh.k8s.task.BaseTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.TaskRunDTO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.statemesh.k8s.util.K8SConstants.TASK_RUN_STATUS_LABEL;

@Slf4j
public class ReadTaskRunStatusTask extends BaseTask<List<TaskRunStatus>> {
    private final Set<TaskRunDTO> tasks;

    public ReadTaskRunStatusTask(ApiStub apiStub,
                                 TaskConfig taskConfig,
                                 String namespace,
                                 Set<TaskRunDTO> tasks) {
        super(apiStub, taskConfig, namespace);
        this.tasks = tasks;
    }

    @Override
    public CompletableFuture<TaskResult<List<TaskRunStatus>>> call() {
        try {
            var requestedTaskIds = this.tasks.stream().map(TaskRunDTO::getInternalName).collect(Collectors.toSet());
            var v1TaskRunList = getApiStub().getTaskRun().list(getNamespace()).throwsApiException().getObject();
            var relevantV1Tasks = v1TaskRunList.getItems().stream()
                .filter(t -> requestedTaskIds.contains(t.getMetadata().getName()))
                .collect(Collectors.toSet());
            List<TaskRunStatus> statuses = relevantV1Tasks.stream().map(this::toTaskRunStatus).toList();
            return CompletableFuture.completedFuture(
                TaskResult.<List<TaskRunStatus>>builder()
                    .success(Boolean.TRUE)
                    .value(statuses)
                    .build()
            );
        } catch (ApiException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private TaskRunStatus toTaskRunStatus(V1TaskRun v1TaskRun) {
        var condition = Optional.ofNullable(v1TaskRun.getStatus())
            .flatMap(status -> status.getConditions().stream()
                .filter(c -> "Succeeded".equals(c.getType()))
                .findFirst()
            ).orElse(null);

        var result = TaskRunStatus.builder()
            .taskId(this.tasks.stream()
                .filter(t -> v1TaskRun.getMetadata().getName().equals(t.getInternalName()))
                .findFirst()
                .map(TaskRunDTO::getId)
                .orElse(null));

        if (v1TaskRun.getStatus() != null) {
            result.startTime(v1TaskRun.getStatus().getStartTime() != null ?
                    v1TaskRun.getStatus().getStartTime().toInstant() : null)
                .podName(v1TaskRun.getStatus().getPodName())
                .container(v1TaskRun.getStatus().getSteps().get(0).getContainer())
                .completionTime(v1TaskRun.getStatus().getCompletionTime() != null ?
                    v1TaskRun.getStatus().getCompletionTime().toInstant() : null);

        }
        if (condition != null) {
            result.stage(condition.getReason());
        }
        if (v1TaskRun.getMetadata() != null && v1TaskRun.getMetadata().getLabels() != null &&
            v1TaskRun.getMetadata().getLabels().containsKey(TASK_RUN_STATUS_LABEL)) {
            result.stage(v1TaskRun.getMetadata().getLabels().get(TASK_RUN_STATUS_LABEL));
            result.completionTime(Instant.now());
        }

        return result.build();
    }
}
