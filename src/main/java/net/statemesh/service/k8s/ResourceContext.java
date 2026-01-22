package net.statemesh.service.k8s;

import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.task.control.ControlTask;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.ApiUtils;
import net.statemesh.service.ApplicationService;
import net.statemesh.service.ContainerService;
import net.statemesh.service.RayJobService;
import net.statemesh.service.TaskRunService;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.ContainerDTO;
import net.statemesh.service.dto.DatabaseDTO;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public abstract class ResourceContext {
    @Builder
    public record Context(ApplicationDTO application, String podName, ContainerDTO container, DatabaseDTO database) {}
    protected final ApplicationService applicationService;
    protected final ContainerService containerService;
    protected final TaskRunService taskRunService;
    protected final RayJobService rayJobService;
    protected final KubernetesController kubernetesController;
    protected final ApplicationProperties applicationProperties;

    protected Context buildContext(ControlTask.ControlObject type, String ...termId) {
        if (ControlTask.ControlObject.APPLICATION == type) {
            return buildAppContext(termId[0], termId[1], termId[2]);
        } else {
            throw new IllegalArgumentException("Unsupported control object type: " + type);
        }
    }

    protected Context buildAppContext(ApplicationDTO application, String podName, String containerId) {
        return Context
            .builder()
            .application(application)
            .podName(podName)
            .container(
                containerId == null ? null :
                    containerService.findOne(containerId)
                        .orElseThrow(() -> new RuntimeException("Container " + containerId + " was not found"))
            )
            .build();
    }

    protected Context buildAppContext(String applicationId, String podName, String containerId) {
        final ApplicationDTO application = applicationService.findOne(applicationId)
            .orElseThrow(EntityNotFoundException::new);
        return buildAppContext(application, podName, containerId);
    }

    protected String key(String... parts) {
        return StringUtils.join(parts, "#");
    }

    protected String endpoint(String... parts) {
        return StringUtils.join(parts, "/");
    }

    protected ApiStub deleteDanglingPods(Context context) {
        if (context.application() == null ||
            context.application().getProject() == null ||
            context.application().getProject().getCluster() == null) {
            return null;
        }

        final ApiStub client = this.kubernetesController.getClients()
            .get(context.application().getProject().getZone().getZoneId())
            .get(context.application().getProject().getCluster().getCid());

        ApiUtils.deleteDanglingPods(
            client,
            context.application().getDeployedNamespace(),
            applicationProperties.getJob().isTerminatingPodsDeleteFinalizers()
        );

        return client;
    }
}
