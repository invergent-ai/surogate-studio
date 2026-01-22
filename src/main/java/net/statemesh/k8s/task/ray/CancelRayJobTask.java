package net.statemesh.k8s.task.ray;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.generic.options.PatchOptions;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.RayJobDTO;

@Slf4j
public class CancelRayJobTask extends BaseMutationTask<Void> {
    final RayJobDTO rayJob;

    public CancelRayJobTask(ApiStub apiStub,
                            TaskConfig taskConfig,
                            String namespace,
                            RayJobDTO rayJob) {
        super(apiStub, taskConfig, namespace);
        this.rayJob = rayJob;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        // TODO - This will work only without clusterSelector
        log.info("Cancelling RayJob...");
        if (!this.rayJobIsCancelled()) {
            getApiStub().getRayJob().patch(
                getNamespace(),
                rayJob.getInternalName(),
                V1Patch.PATCH_FORMAT_JSON_PATCH,
                new V1Patch("[{\"op\":\"replace\",\"path\":\"/spec/suspend\",\"value\":true}]"),
                new PatchOptions()
            ).throwsApiException();
        } else {
            log.debug("Skipping RayJob {} cancel as it's already cancelled", rayJob.getName());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## RayJob cancel:: {} :: wait poll step", rayJob.getName());
        return rayJobIsCancelled();
    }

    private boolean rayJobIsCancelled() {
        try {
            return Boolean.TRUE.equals(
                getApiStub().getRayJob()
                    .get(getNamespace(), rayJob.getInternalName())
                    .throwsApiException()
                    .getObject()
                    .getSpec().getSuspend()
            );
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return true;
            }
            throw new RuntimeException(e);
        }
    }
}
