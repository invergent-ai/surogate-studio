package net.statemesh.k8s.task.security;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PolicyRule;
import io.kubernetes.client.openapi.models.V1Role;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.K8RoleRulesDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static net.statemesh.k8s.util.NamingUtils.rfc1123Name;

public class CreateRoleTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(CreateRoleTask.class);
    protected final String roleName;
    private final List<K8RoleRulesDTO> rules;

    public CreateRoleTask(ApiStub apiStub,
                          TaskConfig taskConfig,
                          String namespace,
                          String roleName,
                          List<K8RoleRulesDTO> rules) {
        super(apiStub, taskConfig, namespace);
        this.roleName = roleName;
        this.rules = rules;
    }

    @Override
    public void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create Role {} if not exists", rfc1123Name(roleName));

        if (!objectExists(rfc1123Name(roleName))) {
            log.debug("Create Role {}", rfc1123Name(roleName));
            getApiStub().getRbacAuthorizationV1Api().createNamespacedRole(
                getNamespace(),
                new V1Role()
                    .metadata(
                        new V1ObjectMeta()
                            .name(rfc1123Name(roleName))
                    )
                    .rules(rules.stream()
                        .map(rule -> new V1PolicyRule()
                            .apiGroups(rule.getApiGroups())
                            .resources(rule.getResources())
                            .verbs(rule.getVerbs()))
                        .toList()
                    )
            ).execute();
        } else {
            log.debug("Skipping Role {} creation as it exists", rfc1123Name(roleName));
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Role :: {} :: wait poll step", getNamespace());
        return objectExists(rfc1123Name(roleName));
    }

    protected boolean objectExists(String name) throws ApiException {
        try {
            V1Role role = getApiStub().getRbacAuthorizationV1Api()
                .readNamespacedRole(name, getNamespace()).execute();
            return name.equals(Objects.requireNonNull(role.getMetadata()).getName());
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
