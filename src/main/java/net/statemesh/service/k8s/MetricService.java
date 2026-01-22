package net.statemesh.service.k8s;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.util.MetricType;
import net.statemesh.service.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.statemesh.config.K8Timeouts.READ_METRICS_TIMEOUT_SECONDS;

@Service
@Slf4j
public class MetricService extends PollingEventStreamService {
    public MetricService(ApplicationService applicationService,
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
    public PollHandle start(Long pollInterval, Long pollTimeout, String... emId) {
        final var appId = emId[0];
        final var podName = emId[1];
        final var containerId = emId[2];

        Context context = buildAppContext(appId, podName, containerId);
        deleteDanglingPods(context);

        log.info("Starting metrics for application {}, pod {}, container {}", appId, podName, containerId);
        super.start(pollInterval, pollTimeout, emId);
        return null;
    }

    @Override
    protected void onTick(String... emId) throws InterruptedException, TimeoutException {
        final var appId = emId[0];
        final var podName = emId[1];
        final var containerId = emId[2];
        final Context context = buildAppContext(appId, podName, containerId);

        try {
            var result = kubernetesController.readMetrics(
                context.application().getDeployedNamespace(),
                context.application().getProject().getCluster(),
                MetricType.CONTAINER,
                null,
                context.application(),
                context.application().getContainers().stream()
                    .filter(container -> containerId.equals(container.getId()))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Application with no right containers")),
                null,
                null,
                Instant.now().atZone(ZoneOffset.UTC).minusMinutes(60).toEpochSecond() * 1000,
                Instant.now().atZone(ZoneOffset.UTC).toEpochSecond() * 1000,
                pollInterval
            ).get(READ_METRICS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.isSuccess() && !Thread.currentThread().isInterrupted()) {
                log.error("Failed to read metrics from Kubernetes for application {}, pod {}, container {}", appId, podName, containerId);
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
