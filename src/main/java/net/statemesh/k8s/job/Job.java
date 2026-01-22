package net.statemesh.k8s.job;

import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.util.ApiStub;

import java.util.concurrent.Executors;
import java.util.function.Consumer;

public abstract class Job {
    protected final KubernetesController kubernetesController;
    protected final ApplicationProperties applicationProperties;

    public Job(KubernetesController kubernetesController,
               ApplicationProperties applicationProperties) {
        this.kubernetesController = kubernetesController;
        this.applicationProperties = applicationProperties;
    }

    protected void multiClusterRun(Consumer<ApiStub> action) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            kubernetesController.getClients().values()
                .forEach(map -> map.values().forEach(
                    apiStub -> executor.submit(
                        () -> action.accept(apiStub)
                    )
                ));
        }
    }
}
