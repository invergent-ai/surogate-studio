package net.statemesh.k8s.task.security;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static net.statemesh.k8s.util.NamingUtils.rfc1123Name;

public class CreateServiceAccountTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(CreateServiceAccountTask.class);
    protected String serviceAccountName;

    public CreateServiceAccountTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String serviceAccountName
    ) {
        super(apiStub, taskConfig, namespace);
        this.serviceAccountName = serviceAccountName;
    }

    @Override
    public void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        if (!objectExists(rfc1123Name(serviceAccountName))) {
            log.debug("Create ServiceAccount {}", rfc1123Name(serviceAccountName));
            getApiStub().getCoreV1Api().createNamespacedServiceAccount(
                getNamespace(),
                new V1ServiceAccount()
                    .metadata(
                        new V1ObjectMeta()
                            .name(rfc1123Name(serviceAccountName))
                    )
            ).execute();
        } else {
            log.debug("Skipping ServiceAccount {} creation because it exists", rfc1123Name(serviceAccountName));
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## ServiceAccount :: {} :: wait poll step", getNamespace());
        return objectExists(rfc1123Name(serviceAccountName));
    }

    protected boolean objectExists(String name) throws ApiException {
        try {
            V1ServiceAccount role = getApiStub().getCoreV1Api()
                .readNamespacedServiceAccount(name, getNamespace()).execute();
            return name.equals(Objects.requireNonNull(role.getMetadata()).getName());
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
