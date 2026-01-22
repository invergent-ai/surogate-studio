package net.statemesh.k8s.task;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.thread.VirtualWait;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class BaseMutationTask<T> extends BaseTask<T> {
    public BaseMutationTask(ApiStub apiStub, TaskConfig taskConfig, String namespace) {
        super(apiStub, taskConfig, namespace);
    }

    @Override
    public CompletableFuture<TaskResult<T>> call() {
        var taskResult = TaskResult.<T>builder();
        try {
            execute(taskResult);
        } catch (SkippedExistsException e) {
            return completeWithSkippedExists();
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        boolean ready = pollReadiness();

        taskResult.success(ready);
        taskResult.waitTimeout(!ready);
        taskResult.creationStatus(TaskResult.CreationStatus.CREATED);

        onSuccess(taskResult, ready);

        return completeWithTaskResult(taskResult.build());
    }

    protected void execute(TaskResult.TaskResultBuilder<T> taskResult) throws Exception {
        throw new UnsupportedOperationException("execute() not implemented in subclass");
    }

    protected boolean isReady() throws ApiException {
        throw new UnsupportedOperationException("isReady() not implemented in subclass");
    }

    protected boolean pollReadiness() {
        return VirtualWait.poll(
            Duration.ofSeconds(getTaskConfig().resourceOperationPollInterval()),
            Duration.ofSeconds(getTaskConfig().resourceOperationWaitTimeout()),
            () -> {
                try {
                    return isReady();
                } catch (ApiException e) {
                    return Boolean.FALSE;
                }
            });
    }

    protected CompletableFuture<TaskResult<T>> completeWithTaskResult(TaskResult<T> taskResult) {
        return CompletableFuture.completedFuture(taskResult);
    }

    protected CompletableFuture<TaskResult<T>> completeWithSkippedExists() {
        return completeWithTaskResult(
            TaskResult.<T>builder()
                .success(true)
                .creationStatus(TaskResult.CreationStatus.SKIPPED_EXISTS)
                .build()
        );
    }

    protected void onSuccess(TaskResult.TaskResultBuilder<T> taskResult, boolean ready) {
        // Optional hook for subclasses to perform actions on success
    }
}
