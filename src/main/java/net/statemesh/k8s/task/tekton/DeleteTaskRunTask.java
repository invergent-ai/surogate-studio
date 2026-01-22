package net.statemesh.k8s.task.tekton;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.TaskRunDTO;

import java.util.Objects;

@Slf4j
public class DeleteTaskRunTask extends CancelTaskRunTask  {
    public DeleteTaskRunTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        TaskRunDTO taskRun) {
        super(apiStub, taskConfig, namespace, taskRun);
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        if (taskRunExists()) {
            getApiStub().getTaskRun().delete(getNamespace(), taskRun.getInternalName()).throwsApiException();
        } else {
            log.debug("Skipping task {} deletion as it does not exist", getNamespace());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Delete Task :: {} :: wait poll step", getNamespace());
        return !taskRunExists();
    }

    protected boolean taskRunExists() {
        var taskRuns = getApiStub().getTaskRun().list(getNamespace()).getObject();
        return taskRuns != null && taskRuns.getItems().stream()
            .anyMatch(t -> taskRun.getInternalName().equals(
                Objects.requireNonNull(t.getMetadata()).getName())
            );
    }
}
