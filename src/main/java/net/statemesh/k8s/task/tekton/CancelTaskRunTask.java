package net.statemesh.k8s.task.tekton;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.generic.options.PatchOptions;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.TaskRunDTO;

@Slf4j
public class CancelTaskRunTask extends BaseMutationTask<Void> {
    final TaskRunDTO taskRun;

    public CancelTaskRunTask(ApiStub apiStub,
                             TaskConfig taskConfig,
                             String namespace,
                             TaskRunDTO taskRun) {
        super(apiStub, taskConfig, namespace);
        this.taskRun = taskRun;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        log.info("Cancelling Task...");
        if (!this.taskRunIsCancelled()) {
            getApiStub().getTaskRun().patch(
                getNamespace(),
                taskRun.getInternalName(),
                V1Patch.PATCH_FORMAT_JSON_PATCH,
                new V1Patch("[{\"op\":\"replace\",\"path\":\"/spec/status\",\"value\":\"TaskRunCancelled\"}]"),
                new PatchOptions()
            ).throwsApiException();
        } else {
            log.debug("Skipping task {} cancel as it's already cancelled", taskRun.getName());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Tekton task cancel:: {} :: wait poll step", taskRun.getName());
        return taskRunIsCancelled();
    }

    private boolean taskRunIsCancelled() {
        try {
            var specStatus = getApiStub().getTaskRun()
                .get(getNamespace(), taskRun.getInternalName())
                .throwsApiException()
                .getObject().getSpec().getStatus();
            return "TaskRunCancelled".equals(specStatus);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return true;
            }
            throw new RuntimeException(e);
        }
    }
}
