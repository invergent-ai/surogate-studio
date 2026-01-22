package net.statemesh.k8s.flow;

import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.DatabaseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class DeleteDatabaseFlow extends BaseDatabaseFlow {
    public DeleteDatabaseFlow(KubernetesController kubernetesController,
                              ClusterService clusterService,
                              ResourceService resourceService,
                              @Qualifier("flowScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(kubernetesController, clusterService, resourceService, taskScheduler);
    }

    @Override
    public TaskResult<String> execute(DatabaseDTO resource) throws K8SException {
        return null;
    }

    @Override
    String selectCluster(DatabaseDTO resource, KubernetesController kubernetesController) {
        return "";
    }
}
