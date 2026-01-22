package net.statemesh.k8s.task.control;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.Streams;
import net.statemesh.k8s.task.BaseTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.ContainerDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static net.statemesh.k8s.util.K8SConstants.SHELL_CONSOLE_INIT;
import static net.statemesh.k8s.util.K8SConstants.SHELL_CONSOLE_SH;

public class AppShellTask extends BaseTask<Void> {
    private final Logger log = LoggerFactory.getLogger(AppShellTask.class);

    private final ApplicationDTO application;
    private final String podName;
    private final ContainerDTO container;
    private final OutputStream outputStream;
    private final InputStream inputStream;
    private final Integer columns;
    private final AsyncTaskExecutor taskExecutor;

    public AppShellTask(ApiStub apiStub,
                        TaskConfig taskConfig,
                        String namespace,
                        ApplicationDTO application,
                        String podName,
                        ContainerDTO container,
                        OutputStream outputStream,
                        InputStream inputStream,
                        Integer columns,
                        AsyncTaskExecutor taskExecutor) {
        super(apiStub, taskConfig, namespace);
        this.application = application;
        this.podName = podName;
        this.container = container;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.columns = columns;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public CompletableFuture<TaskResult<Void>> call() {
        log.info("Get shell for application {}", application.getInternalName());
        deleteDanglingPods();

        try {
            Process proc = getApiStub().getExec().exec(
                getNamespace(),
                this.podName,
                new String[] {SHELL_CONSOLE_SH},
                NamingUtils.containerName(application.getInternalName(), container.getImageName()),
                Boolean.TRUE,
                Boolean.TRUE
            );
            if (columns != null) {
                proc.getOutputStream().write(String.format(SHELL_CONSOLE_INIT, columns).getBytes());
            }

            taskExecutor.submit(() -> {
                try {
                    Streams.copy(inputStream, proc.getOutputStream());
                } catch (IOException ex) {
                    // Ignore
                }
            });

            var out = taskExecutor.submit(() -> {
                try {
                    Streams.copy(proc.getInputStream(), outputStream);
                } catch (IOException ex) {
                    // Ignore
                }
            });

            var err = taskExecutor.submit(() -> {
                try {
                    Streams.copy(proc.getErrorStream(), outputStream);
                } catch (IOException ex) {
                    // Ignore
                }
            });

            proc.waitFor();
            out.get();
            err.get();
            proc.destroy();
        } catch (ApiException | IOException | InterruptedException | ExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }

        return CompletableFuture.completedFuture(TaskResult.success());
    }
}
