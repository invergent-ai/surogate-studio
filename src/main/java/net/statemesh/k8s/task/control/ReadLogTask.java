package net.statemesh.k8s.task.control;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.Streams;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.task.BaseTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ReadLogTask extends BaseTask<InputStream> {
    private final OutputStream outputStream;
    private final Integer tailLines;
    private final Integer sinceSeconds;
    private final String specificPodName;
    private final String specificContainerName;

    public ReadLogTask(ApiStub apiStub,
                       TaskConfig taskConfig,
                       String namespace,
                       OutputStream outputStream,
                       Integer tailLines,
                       Integer sinceSeconds,
                       String specificPodName,
                       String specificContainerName) {
        super(apiStub, taskConfig, namespace);
        this.outputStream = outputStream;
        this.tailLines = tailLines;
        this.sinceSeconds = sinceSeconds;
        this.specificPodName = specificPodName;
        this.specificContainerName = specificContainerName;
    }

    @Override
    public CompletableFuture<TaskResult<InputStream>> call() {
        deleteDanglingPods();

        try {
            InputStream inputStream =
                getApiStub().getLogs().streamNamespacedPodLog(
                    getNamespace(),
                    specificPodName,
                    specificContainerName,
                    sinceSeconds,
                    tailLines,
                    Boolean.TRUE
                );

            if (outputStream == null) {
                return CompletableFuture.completedFuture(
                    TaskResult.<InputStream>builder()
                        .success(Boolean.TRUE)
                        .value(inputStream)
                        .build()
                );
            }

            Streams.copy(inputStream, outputStream);
        } catch (IOException | ApiException e) {
            return CompletableFuture.failedFuture(e);
        }

        return CompletableFuture.completedFuture(
            TaskResult.success()
        );
    }
}
