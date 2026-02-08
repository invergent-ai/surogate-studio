package net.statemesh.service.k8s;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.service.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.statemesh.config.K8Timeouts.READ_METRICS_TIMEOUT_SECONDS;

@Service
@Slf4j
public class RayJobMetricService extends PollingEventStreamService {
    public RayJobMetricService(ApplicationService applicationService,
                               ContainerService containerService,
                               TaskRunService taskRunService,
                               RayJobService rayJobService,
                               KubernetesController kubernetesController,
                               ApplicationProperties applicationProperties,
                               @Qualifier("statusScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(applicationService, containerService, taskRunService, rayJobService,
            kubernetesController, applicationProperties, taskScheduler);
    }

    @Override
    protected void onTick(String... emId) throws InterruptedException, TimeoutException {
        var rayJob = rayJobService.findRayJobsById(emId).stream().findAny().orElse(null);
        if (rayJob == null) {
            return;
        }

        try {
            var result = kubernetesController.readAimMetrics(
                rayJob.getProject().getCluster(),
                rayJob.getProject().getRayCluster(),
                rayJob.getInternalName(),
                rayJob.getUseAxolotl()
            ).get(READ_METRICS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.isSuccess() && !Thread.currentThread().isInterrupted()) {
                log.error("Failed to read metrics from Kubernetes for ray job {}", rayJob.getInternalName());
                return;
            }

            sendEvent("metrics", result.getValue(), emId);
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ApiException)) {
                throw new RuntimeException(e);
            } else {
                log.trace("Error reading metrics with message {}", e.getMessage());
            }
        }
    }
}
