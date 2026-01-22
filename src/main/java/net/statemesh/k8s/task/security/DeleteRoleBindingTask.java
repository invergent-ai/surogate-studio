package net.statemesh.k8s.task.security;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.statemesh.k8s.util.NamingUtils.rfc1123Name;

public class DeleteRoleBindingTask extends CreateRoleBindingTask {
    private final Logger log = LoggerFactory.getLogger(DeleteRoleBindingTask.class);

    public DeleteRoleBindingTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String roleBindingName) {
        super(apiStub, taskConfig, namespace, roleBindingName, null, null);
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder taskResult) throws Exception {
        if (objectExists(rfc1123Name(roleBindingName))) {
            log.debug("Delete RoleBinding {}", roleBindingName);
            getApiStub().getRbacAuthorizationV1Api()
                .deleteNamespacedRoleBinding(
                    rfc1123Name(roleBindingName),
                    getNamespace())
                .execute();
        } else {
            log.debug("Skipping RoleBinding {} deletion as it does not exist", roleBindingName);
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## RoleBinding :: {} :: wait poll step", getNamespace());
        return !objectExists(rfc1123Name(roleBindingName));
    }
}
