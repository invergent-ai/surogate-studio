package net.statemesh.k8s.task.security;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.statemesh.k8s.util.NamingUtils.rfc1123Name;

public class DeleteServiceAccountTask extends CreateServiceAccountTask {
    private final Logger log = LoggerFactory.getLogger(DeleteServiceAccountTask.class);

    public DeleteServiceAccountTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String serviceAccountName) {
        super(apiStub, taskConfig, namespace, serviceAccountName);
    }

    @Override
    public void execute(TaskResult.TaskResultBuilder taskResult) throws Exception {
        if (objectExists(rfc1123Name(serviceAccountName))) {
            log.debug("Delete ServiceAccount {}", rfc1123Name(serviceAccountName));
            getApiStub().getCoreV1Api().deleteNamespacedServiceAccount(
                rfc1123Name(serviceAccountName),
                getNamespace()
            ).execute();
        } else {
            log.debug("Skipping ServiceAccount {} deletion because it does not exist", rfc1123Name(serviceAccountName));
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## ServiceAccount :: {} :: wait poll step", getNamespace());
        return !objectExists(rfc1123Name(serviceAccountName));
    }
}
