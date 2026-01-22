package net.statemesh.k8s.task.ray;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.crd.rayjob.models.V1RayJobList;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.RayJobDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class DeleteRayJobTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeleteRayJobTask.class);

    private final RayJobDTO rayJob;

    public DeleteRayJobTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        RayJobDTO rayJob
    ) {
        super(apiStub, taskConfig, namespace);
        this.rayJob = rayJob;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        log.info("Deleting ray job {} if exists", rayJob.getName());
        if (rayJobExists()) {
            log.debug("Delete ray job {}", rayJob.getName());
            getApiStub().getRayJob().delete(getNamespace(), rayJob.getInternalName());
        } else {
            log.debug("Skipping ray job {} deletion as it does not exist", rayJob.getName());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Ray job delete :: {} :: wait poll step", rayJob.getName());
        return !rayJobExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("Ray job deleted successfully [{}]", ready);
    }

    private boolean rayJobExists() {
        V1RayJobList jobs = getApiStub().getRayJob().list(getNamespace()).getObject();
        return jobs != null && jobs.getItems().stream()
            .anyMatch(job -> rayJob.getInternalName().equals(
                Objects.requireNonNull(job.getMetadata()).getName())
            );
    }
}
