package net.statemesh.k8s.task.application;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateNamespaceTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(CreateNamespaceTask.class);

    public CreateNamespaceTask(ApiStub apiStub,
                               TaskConfig taskConfig,
                               String namespace) {
        super(apiStub, taskConfig, namespace);
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create namespace {} if not exists", getNamespace());

        if (!namespaceExists()) {
            log.debug("Create namespace {}", getNamespace());
            getApiStub().getCoreV1Api()
                .createNamespace(
                    new V1Namespace()
                        .metadata(
                            new V1ObjectMeta().name(getNamespace()))
                )
                .execute();
        } else {
            log.debug("Skipping namespace {} creation as it exists", getNamespace());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Namespace :: {} :: wait poll step", getNamespace());
        return namespaceExists();
    }

    protected boolean namespaceExists() throws ApiException {
        try {
            V1NamespaceList namespaces =
                getApiStub().getCoreV1Api().listNamespace().execute();
            return namespaces.getItems().stream()
                .anyMatch(namespace -> getNamespace().equals(namespace.getMetadata().getName()));
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
