package net.statemesh.service;

import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.TimeoutException;

public abstract class EventStreamService extends PollingEventStreamService {
    private volatile boolean hasRun = false;
    private PollHandle handle;

    public EventStreamService(
        ApplicationService applicationService,
        ContainerService containerService,
        TaskRunService taskRunService,
        RayJobService rayJobService,
        KubernetesController kubernetesController,
        ApplicationProperties applicationProperties,
        ThreadPoolTaskScheduler taskScheduler) {
        super(applicationService, containerService, taskRunService, rayJobService, kubernetesController, applicationProperties, taskScheduler);
    }

    @Override
    public PollHandle start(Long pollInterval, Long pollTimeout, String... emId) {
        handle = super.start(pollInterval, pollTimeout, emId);
        hasRun = false;
        return handle;
    }

    @Override
    protected void onTick(String... emId) throws InterruptedException, TimeoutException {
        if (!hasRun) {
            hasRun = true;
            try {
                run(emId);
            } finally {
                stop(emId);
            }
        }
    }


    @Override
    public void stop(String... emId) {
        if (handle.future != null && !handle.future.isCancelled()) {
            handle.future.cancel(false);
        }
        super.stop(emId);
    }

    protected abstract void run(String... emId) throws InterruptedException, TimeoutException;

}
