package net.statemesh.k8s.job;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static net.statemesh.k8s.util.ApiUtils.deleteDanglingPods;

@Component
@Slf4j
public class DanglingPodsCleanupJob extends Job {
    public DanglingPodsCleanupJob(KubernetesController kubernetesController,
                                  ApplicationProperties applicationProperties) {
        super(kubernetesController, applicationProperties);
    }

    @Scheduled(cron = "${app.job.delete-terminating-schedule}")
    public void cleanTerminatingPods() {
        log.info("Running cleanup pods job");
        multiClusterRun(this::cleanupPods);
        log.info("Finished cleanup pods job");
    }

    private void cleanupPods(ApiStub client) {
        deleteDanglingPods(client, null, applicationProperties.getJob().isTerminatingPodsDeleteFinalizers());
    }
}
