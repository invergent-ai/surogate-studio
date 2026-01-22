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
public class GpuMetricService extends PollingEventStreamService {
    private final NodeService nodeService;

    public GpuMetricService(ApplicationService applicationService,
                            ContainerService containerService,
                            TaskRunService taskRunService,
                            RayJobService rayJobService,
                            KubernetesController kubernetesController,
                            ApplicationProperties applicationProperties,
                            @Qualifier("statusScheduler")ThreadPoolTaskScheduler taskScheduler,
                            NodeService nodeService) {
        super(applicationService, containerService, taskRunService, rayJobService, kubernetesController, applicationProperties, taskScheduler);
        this.nodeService = nodeService;
    }

    @Override
    protected void onTick(String... emId) throws InterruptedException, TimeoutException {
        final var nodeId = emId[0];
        final var gpuId = emId[1];
        try {
            var node = nodeService.findOne(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Node " + nodeId + " does not exist"));

            var result = kubernetesController.readMetrics(
                null,
                kubernetesController.getClusterService().getApplianceCluster(),
                MetricType.GPU,
                node,
                null,
                null,
                Integer.parseInt(gpuId),
                null,
                Instant.now().atZone(ZoneOffset.UTC).minusMinutes(60).toEpochSecond() * 1000,
                Instant.now().atZone(ZoneOffset.UTC).toEpochSecond() * 1000,
                pollInterval
            ).get(READ_METRICS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.isSuccess() && !Thread.currentThread().isInterrupted()) {
                log.error("Failed to read GPU metrics in Kubernetes for node {} and GPU {}", nodeId, gpuId);
                return;
            }

            sendEvent("metrics", result.getValue(), emId);
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ApiException)) {
                throw new RuntimeException(e);
            } else {
                log.trace("Error reading app status with message {}", e.getMessage());
            }
        }
    }
}
