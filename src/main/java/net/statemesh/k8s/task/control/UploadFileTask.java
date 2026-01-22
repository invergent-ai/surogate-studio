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
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static net.statemesh.k8s.util.ApiUtils.podName;

public class UploadFileTask extends BaseTask {
    private final Logger log = LoggerFactory.getLogger(UploadFileTask.class);

    private final ApplicationDTO application;
    private final String podName;
    private final ContainerDTO container;
    private final String sourcePath;
    private final String destinationPath;

    public UploadFileTask(ApiStub apiStub,
                          TaskConfig taskConfig,
                          String namespace,
                          ApplicationDTO application,
                          String podName,
                          ContainerDTO container,
                          String sourcePath,
                          String destinationPath) {
        super(apiStub, taskConfig, namespace);
        this.application = application;
        this.podName = podName;
        this.container = container;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
    }

    @Override
    public CompletableFuture<TaskResult<Void>> call() {
        log.debug("Uploading file to application {}", application.getName());
        try {
            new Copy(getApiStub().getApiClient()).copyFileToPod(
                getNamespace(),
                podName,
                container != null ?
                        NamingUtils.containerName(application.getInternalName(), container.getImageName()) :
                        null,
                Paths.get(sourcePath),
                Paths.get(destinationPath)
            );
        } catch (ApiException | IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        return CompletableFuture.completedFuture(
            TaskResult.success()
        );
    }
}
