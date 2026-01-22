package net.statemesh.k8s.task.application.storage;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeletePersistentVolumeClaimTask extends PersistentVolumeClaimTask {
    private final Logger log = LoggerFactory.getLogger(DeletePersistentVolumeClaimTask.class);
    private final String pvcName;

    public DeletePersistentVolumeClaimTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String pvcName
    ) {
        super(apiStub, taskConfig, namespace, null, null);
        this.pvcName = pvcName;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Deleting PVC {} if exists", pvcName);
        if (pvcExists(pvcName)) {
            log.debug("Delete PVC {}", pvcName);
            getApiStub().getCoreV1Api()
                .deleteNamespacedPersistentVolumeClaim(pvcName, getNamespace()).execute();
        } else {
            log.debug("Skipping PVC {} deletion as it does not exist", pvcName);
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## PVC delete :: {} :: wait poll step", pvcName);
        return !pvcExists(pvcName);
    }
}
