package net.statemesh.k8s.task.secret;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Secret;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteSecretTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeleteSecretTask.class);

    private final String name;

    public DeleteSecretTask(
        String name,
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace
    ) {
        super(apiStub, taskConfig, namespace);
        this.name = name;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        log.info("Deleting secret {} if exists", name);
        if(secretExists(name)) {
            log.debug("Delete secret {}", name);
            getApiStub().getCoreV1Api()
                .deleteNamespacedSecret(
                    name,
                    getNamespace()
                )
                .execute();
        } else {
            log.debug("Skipping secret {} deletion as it does not exist", name);
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Secret delete :: {} :: wait poll step", name);
        return !secretExists(name);
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("Secret deleted successfully [{}]", ready);
    }

    protected boolean secretExists(String name) throws ApiException {
        try {
            V1Secret secret = getApiStub().getCoreV1Api().readNamespacedSecret(name, getNamespace()).execute();
            return secret.getData() != null;
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return Boolean.FALSE;
            }
            throw e;
        }
    }
}
