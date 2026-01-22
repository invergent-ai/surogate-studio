package net.statemesh.k8s.flow;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.ApplicationDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CreateApplicationFlow extends BaseApplicationFlow {
    public CreateApplicationFlow(
        KubernetesController kubernetesController,
        ClusterService clusterService,
        ResourceService resourceService,
        @Qualifier("flowScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(kubernetesController, clusterService, resourceService, taskScheduler);
    }

    @Override
    public TaskResult<String> execute(ApplicationDTO resource) throws K8SException {
        return executeCreate(resource);
    }
}
