package net.statemesh.k8s.task.network;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.crd.traefik.models.V1alpha1MiddlewareTCPList;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.thread.VirtualWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DeleteMiddlewareTCPTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeleteMiddlewareTCPTask.class);

    private final String middleware;

    public DeleteMiddlewareTCPTask(
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
        log.info("Deleting middleware TCP {} if exists", middleware);
        if (middlewareTCPExists()) {
            log.debug("Delete middleware TCP {}", middleware);
            getApiStub().getTraefikMiddlewareTCP().delete(getNamespace(), middleware);
        } else {
            log.debug("Skipping middleware TCP {} deletion as it does not exist", middleware);
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Middleware TCP delete :: {} :: wait poll step", middleware);
        return !middlewareTCPExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("Middleware TCP deleted successfully [{}]", ready);
    }

    private boolean middlewareTCPExists() {
        V1alpha1MiddlewareTCPList middlewares =
            getApiStub().getTraefikMiddlewareTCP().list(getNamespace()).getObject();

        return middlewares.getItems().stream()
            .anyMatch(mid -> middleware.equals(mid.getMetadata().getName()));
    }
}
