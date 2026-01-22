package net.statemesh.k8s.flow;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.task.tekton.TaskSpecs;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.dto.TaskRunDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class TaskRunFlow extends ResourceCreationFlow<TaskRunDTO> {
    private final TaskSpecs taskSpecs;

    public TaskRunFlow(KubernetesController kubernetesController,
                       ClusterService clusterService,
                       ResourceService resourceService,
                       @Qualifier("flowScheduler") ThreadPoolTaskScheduler taskScheduler,
                       TaskSpecs taskSpecs) {
        super(kubernetesController, clusterService, resourceService, taskScheduler);
        this.taskSpecs = taskSpecs;
    }

    @Override
    public TaskResult<String> execute(TaskRunDTO resource) throws K8SException {
        return executeCreate(resource);
    }

    @Override
    CompletableFuture<TaskResult<String>> createDeployment(TaskRunDTO taskRun, ClusterDTO cluster) {
        return kubernetesController.runTask(cluster, taskRun, taskSpecs, getNamespace(taskRun));
    }

    @Override
    void setPublicHostname(TaskRunDTO resource, ClusterDTO cluster, KubernetesController kubernetesController) {
        // do nothing
    }

    @Override
    String getNamespace(TaskRunDTO taskRun) {
        return !StringUtils.isEmpty(taskRun.getDeployedNamespace()) ?
            taskRun.getDeployedNamespace() :
            taskRun.getProject().getNamespace();
    }

    @Override
    String selectCluster(TaskRunDTO resource, KubernetesController kubernetesController) {
        return kubernetesController
            .selectClusterForTaskRun(resource.getProject().getZone().getZoneId());
    }
}
