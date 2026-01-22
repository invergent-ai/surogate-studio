package net.statemesh.k8s.job;

import com.google.common.collect.Streams;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.config.Constants;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.util.ApiUtils;
import net.statemesh.service.ApplicationService;
import net.statemesh.service.DatabaseService;
import net.statemesh.service.dto.ResourceCostDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
@Profile(Constants.PROFILE_CLOUD)
public class ComputeResourceUptimeJob extends Job {
    private final ApplicationService applicationService;
    private final DatabaseService databaseService;

    public ComputeResourceUptimeJob(KubernetesController kubernetesController,
                                    ApplicationProperties applicationProperties,
                                    ApplicationService applicationService,
                                    DatabaseService databaseService) {
        super(kubernetesController, applicationProperties);
        this.applicationService = applicationService;
        this.databaseService = databaseService;
    }

    @Scheduled(fixedRate = 24, initialDelay = 1, timeUnit = TimeUnit.HOURS)
    public void computeResourceUptimeJob() {
        log.info("Computing resource uptime job started");
        final Map<String, List<ResourceCostDTO>> resourcesByCluster =
            Streams.concat(
                applicationService.prepareOpenCostRequests(),
                databaseService.prepareOpenCostRequests()
            ).collect(Collectors.groupingBy(ResourceCostDTO::getClusterId));


        multiClusterRun(apiStub -> {
            try {
                var clusterResources = resourcesByCluster.getOrDefault(apiStub.getCluster().getId(), List.of());
                ApiUtils.getRunningPods(apiStub).getItems().stream()
                    .filter(pod ->
                        clusterResources.stream()
                            .anyMatch(resource -> resource.getInternalName().equals(pod.getMetadata().getName()))
                    )
                    .forEach(pod ->
                        this.computeResourceUptime(
                            pod,
                            clusterResources.stream()
                                .filter(resource -> resource.getInternalName().equals(pod.getMetadata().getName()))
                                .findFirst()
                                .get()
                        )
                    );
            } catch (ApiException e) {
                log.error("Error getting pods for cluster {}", apiStub.getCluster().getName(), e);
            }
        });
        log.info("Computing resource uptime job finished");
    }

    private void computeResourceUptime(V1Pod pod, ResourceCostDTO resource) {
        if (!"Running".equalsIgnoreCase(pod.getStatus().getPhase())) {
            return;
        }

        Optional<V1PodCondition> readyCondition = pod.getStatus().getConditions().stream()
            .filter(condition -> "Ready".equals(condition.getType()) ||
                "PodReady".equals(condition.getType()))
            .filter(condition -> "True".equals(condition.getStatus()))
            .findFirst();

        if (readyCondition.isEmpty() || readyCondition.get().getLastTransitionTime() == null) {
            return;
        }
    }
}
