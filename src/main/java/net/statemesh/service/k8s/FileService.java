package net.statemesh.service.k8s;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.ApplicationService;
import net.statemesh.service.ContainerService;
import net.statemesh.service.RayJobService;
import net.statemesh.service.TaskRunService;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.statemesh.config.K8Timeouts.COPY_FILE_TIMEOUT_SECONDS;

@Service
@Slf4j
public class FileService extends ResourceContext {
    public FileService(ApplicationService applicationService,
                       ContainerService containerService,
                       TaskRunService taskRunService,
                       RayJobService rayJobService,
                       KubernetesController kubernetesController,
                       ApplicationProperties applicationProperties) {
        super(applicationService, containerService, taskRunService, rayJobService, kubernetesController, applicationProperties);
    }

    public boolean uploadFile(String applicationId, String podName, String containerId, String sourcePath, String destinationPath) {
        log.info("Uploading file to application {} and container {}", applicationId, containerId);
        Context context = buildAppContext(applicationId, podName, containerId);

        try {
            var result = kubernetesController.copyFileToPod(
                context.application().getDeployedNamespace(),
                context.application().getProject().getCluster(),
                context.application(),
                podName,
                StringUtils.isEmpty(containerId) ? null :
                    context.application().getContainers().stream()
                        .filter(container -> containerId.equals(container.getId()))
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("Application with no right containers")),
                sourcePath,
                destinationPath
            ).get(COPY_FILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.isSuccess()) {
                throw new RuntimeException(String.format(
                    "Failed to copy file %s for application %s container %s",
                    sourcePath, applicationId, context.container().getImageName()
                ));
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new RuntimeException(String.format(
                "Failed to copy file %s for application %s container %s",
                sourcePath, applicationId, context.container().getImageName()
            ), e);
        }

        return true;
    }

    public InputStream downloadFile(String applicationId, String podName, String containerId, String sourcePath) {
        log.info("Downloading file from application {} and container {}", applicationId, containerId);
        Context context = buildAppContext(applicationId, podName, containerId);

        TaskResult<InputStream> result;
        try {
            result = kubernetesController.copyFileFromPod(
                context.application().getDeployedNamespace(),
                context.application().getProject().getCluster(),
                context.application(),
                podName,
                StringUtils.isEmpty(containerId) ? null :
                    context.application().getContainers().stream()
                        .filter(container -> containerId.equals(container.getId()))
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("Application with no right containers")),
                sourcePath
            ).get(COPY_FILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.isSuccess()) {
                throw new RuntimeException(String.format(
                    "Failed to copy file %s for application %s container %s",
                    sourcePath, applicationId, context.container().getImageName()
                ));
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new RuntimeException(String.format(
                "Failed to copy file %s for application %s container %s",
                sourcePath, applicationId, context.container().getImageName()
            ), e);
        }

        return result.getValue();
    }
}
