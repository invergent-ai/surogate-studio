package net.statemesh.k8s.task.application;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class DeleteNamespaceTask extends CreateNamespaceTask {
    public DeleteNamespaceTask(ApiStub apiStub,
                               TaskConfig taskConfig,
                               String namespace) {
        super(apiStub, taskConfig, namespace);
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        if (namespaceExists()) {
            getApiStub().getCoreV1Api().deleteNamespace(getNamespace())
                .execute();
        } else {
            log.debug("Skipping namespace {} deletion as it does not exist", getNamespace());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Namespace :: {} :: wait poll step", getNamespace());
        return !namespaceExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("Namespace {} deleted", getNamespace());
    }
}
