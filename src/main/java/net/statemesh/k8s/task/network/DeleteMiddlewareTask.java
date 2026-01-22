package net.statemesh.k8s.task.network;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.crd.traefik.models.V1alpha1MiddlewareList;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.thread.VirtualWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DeleteMiddlewareTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeleteMiddlewareTask.class);

    private final String middleware;

    public DeleteMiddlewareTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String middleware
    ) {
        super(apiStub, taskConfig, namespace);
        this.middleware = middleware;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Deleting middleware {} if exists", middleware);
        if (middlewareExists()) {
            log.debug("Delete middleware {}", middleware);
            getApiStub().getTraefikMiddleware().delete(getNamespace(), middleware);
        } else {
            log.debug("Skipping middleware {} deletion as it does not exist", middleware);
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Middleware delete :: {} :: wait poll step", middleware);
        return !middlewareExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("Middleware deleted successfully [{}]", ready);
    }

    private boolean middlewareExists() {
        V1alpha1MiddlewareList middlewares =
            getApiStub().getTraefikMiddleware().list(getNamespace()).getObject();

        return middlewares.getItems().stream()
            .anyMatch(mid -> middleware.equals(mid.getMetadata().getName()));
    }
}
