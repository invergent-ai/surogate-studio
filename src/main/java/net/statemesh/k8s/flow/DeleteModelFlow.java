package net.statemesh.k8s.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiUtils;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.ApplicationDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

import static net.statemesh.config.K8Timeouts.DELETE_FLOW_TIMEOUT_SECONDS;
import static net.statemesh.k8s.flow.CreateModelFlow.*;
import static org.springframework.util.concurrent.FutureUtils.callAsync;

@Component
@Slf4j
public class DeleteModelFlow extends BaseApplicationFlow {
    private final ObjectMapper objectMapper;

    public DeleteModelFlow(
        KubernetesController kubernetesController,
        ClusterService clusterService,
        ResourceService resourceService,
        ObjectMapper objectMapper,
        @Qualifier("flowScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(kubernetesController, clusterService, resourceService, taskScheduler);
        this.objectMapper = objectMapper;
    }

    @Override
    public TaskResult<String> execute(ApplicationDTO rootApplication) {
        log.info("Running delete model flow.");
        final var cluster = setCluster(rootApplication, kubernetesController, clusterService);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var routerTask = callAsync(() -> executeDelete(scaffoldRouterApplication(rootApplication, objectMapper)), executor);
            var workerTask = callAsync(() -> executeDelete(scaffoldWorkerApplication(rootApplication, objectMapper)), executor);
            var cacheTask = callAsync(() -> executeDelete(scaffoldCacheApplication(rootApplication, objectMapper)), executor);

            try {
                CompletableFuture.allOf(routerTask, workerTask, cacheTask)
                    .get(DELETE_FLOW_TIMEOUT_SECONDS * 3, TimeUnit.SECONDS);
                return TaskResult.success();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("Error during deployment execution: {}", e.getCause().getMessage());
                return TaskResult.fail();
            } finally {
                ApiUtils.deleteDanglingPods(
                    kubernetesController.getApi(cluster),
                    getNamespace(rootApplication),
                    true);
            }
        }
    }
}
