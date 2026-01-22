package net.statemesh.k8s.task.application;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ServiceList;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class DeleteServiceTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeleteServiceTask.class);
    private final String serviceName;

    public DeleteServiceTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String serviceName
    ) {
        super(apiStub, taskConfig, namespace);
        this.serviceName = serviceName;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Deleting service {} if exists", serviceName);

        if (serviceExists()) {
            log.debug("Delete service {}", serviceName);
            getApiStub().getCoreV1Api()
                .deleteNamespacedService(serviceName, getNamespace()).execute();
        } else {
            log.debug("Skipping service {} deletion as it does not exist", serviceName);
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Service delete :: {} :: wait poll step", serviceName);
        return !serviceExists();
    }

    private boolean serviceExists() throws ApiException {
        try {
            V1ServiceList services =
                getApiStub().getCoreV1Api().listNamespacedService(getNamespace()).execute();
            return services.getItems().stream()
                .anyMatch(
                    service -> serviceName.equals(Objects.requireNonNull(service.getMetadata()).getName()));
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
