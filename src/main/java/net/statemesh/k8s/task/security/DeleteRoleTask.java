package net.statemesh.k8s.task.security;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.statemesh.k8s.util.NamingUtils.rfc1123Name;

public class DeleteRoleTask extends CreateRoleTask {
    private final Logger log = LoggerFactory.getLogger(DeleteRoleTask.class);

    public DeleteRoleTask(ApiStub apiStub, TaskConfig taskConfig, String namespace, String roleName) {
        super(apiStub, taskConfig, namespace, roleName, null);
    }

    @Override
    public void execute(TaskResult.TaskResultBuilder taskResult) throws ApiException, SkippedExistsException {
        log.info("Delete Role {} if exists", rfc1123Name(roleName));

        if (objectExists(rfc1123Name(roleName))) {
            log.debug("Delete Role {}", rfc1123Name(roleName));
            getApiStub().getRbacAuthorizationV1Api()
                .deleteNamespacedRole(rfc1123Name(roleName), getNamespace())
                .execute();
        } else {
            log.debug("Skipping Role {} deletion as it does not exist", rfc1123Name(roleName));
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Role :: {} :: wait poll step", getNamespace());
        return !objectExists(rfc1123Name(roleName));
    }
}
