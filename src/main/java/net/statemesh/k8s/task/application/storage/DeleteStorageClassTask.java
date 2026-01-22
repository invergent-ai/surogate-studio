package net.statemesh.k8s.task.application.storage;

import com.google.gson.JsonElement;
import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.statemesh.k8s.util.K8SConstants.*;

public class DeleteStorageClassTask extends StorageClassTask {
    private final Logger log = LoggerFactory.getLogger(DeleteStorageClassTask.class);
    private final String scName;

    public DeleteStorageClassTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String scName
    ) {
        super(apiStub, taskConfig, namespace, null);
        this.scName = scName;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        if (storageClassExists(scName)) {
            log.debug("Delete StorageClass {}", scName);
            getApiStub().getApiClient().execute(
                getApiStub().getCustomApi()
                    .deleteClusterCustomObject(STORAGE_CLASS_GROUP, API_VERSION, STORAGE_CLASS_PLURAL, scName)
                    .buildCall(null),
                JsonElement.class
            );
        } else {
            log.debug("Skipping StorageClass {} deletion as it does not exist", scName);
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## StorageClass delete :: {} :: wait poll step", scName);
        return !storageClassExists(scName);
    }
}
