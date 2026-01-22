package net.statemesh.k8s.task.control;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.hack.client.Copy;
import net.statemesh.k8s.task.BaseTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.ContainerDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public class DownloadFileTask extends BaseTask<InputStream> {
    private final Logger log = LoggerFactory.getLogger(DownloadFileTask.class);

    private final ApplicationDTO application;
    private final String podName;
    private final ContainerDTO container;
    private final String sourcePath;

    public DownloadFileTask(ApiStub apiStub,
                            TaskConfig taskConfig,
                            String namespace,
                            ApplicationDTO application,
                            String podName,
                            ContainerDTO container,
                            String sourcePath) {
        super(apiStub, taskConfig, namespace);
        this.application = application;
        this.podName = podName;
        this.container = container;
        this.sourcePath = sourcePath;
    }

    @Override
    public CompletableFuture<TaskResult<InputStream>> call() {
        log.debug("Downloading file from application {}", application.getName());
        try {
            InputStream stream = new Copy(getApiStub().getApiClient()).copyFileFromPod(
                getNamespace(),
                podName,
                container != null ?
                        NamingUtils.containerName(application.getInternalName(), container.getImageName()) :
                        null,
                sourcePath
            );

            return CompletableFuture.completedFuture(
                TaskResult.<InputStream>builder()
                    .success(Boolean.TRUE)
                    .value(stream)
                    .build()
            );
        } catch (ApiException | IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
