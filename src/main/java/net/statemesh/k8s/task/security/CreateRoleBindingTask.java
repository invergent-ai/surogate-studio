package net.statemesh.k8s.task.security;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.RbacV1Subject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1RoleRef;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static net.statemesh.k8s.util.NamingUtils.rfc1123Name;

public class CreateRoleBindingTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(CreateRoleBindingTask.class);
    protected final String roleBindingName;
    private final String subject;
    private final String roleRef;

    public CreateRoleBindingTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String roleBindingName,
        String subject,
        String roleRef
    ) {
        super(apiStub, taskConfig, namespace);
        this.roleBindingName = roleBindingName;
        this.subject = subject;
        this.roleRef = roleRef;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        if (!objectExists(rfc1123Name(roleBindingName))) {
            log.debug("Create RoleBinding {}", rfc1123Name(roleBindingName));
            getApiStub().getRbacAuthorizationV1Api().createNamespacedRoleBinding(
                getNamespace(),
                new V1RoleBinding()
                    .metadata(
                        new V1ObjectMeta()
                            .name(rfc1123Name(roleBindingName))
                    )
                    .subjects(List.of(
                        new RbacV1Subject()
                            .kind("ServiceAccount")
                            .name(subject)
                            .namespace(getNamespace())
                        )
                    )
                    .roleRef(
                        new V1RoleRef()
                            .kind("Role")
                            .name(roleRef)
                            .apiGroup("rbac.authorization.k8s.io")
                    )
            ).execute();
        } else {
            log.debug("Skipping RoleBinding {} creation because it exists", rfc1123Name(roleBindingName));
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## RoleBinding :: {} :: wait poll step", getNamespace());
        return objectExists(rfc1123Name(roleBindingName));
    }

    protected boolean objectExists(String name) throws ApiException {
        try {
            V1RoleBinding role = getApiStub().getRbacAuthorizationV1Api()
                .readNamespacedRoleBinding(name, getNamespace()).execute();
            return name.equals(Objects.requireNonNull(role.getMetadata()).getName());
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
