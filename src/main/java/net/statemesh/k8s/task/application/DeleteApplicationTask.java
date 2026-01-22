package net.statemesh.k8s.task.application;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.ApplicationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteApplicationTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeleteApplicationTask.class);
    private final ApplicationDTO application;

    public DeleteApplicationTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        ApplicationDTO application
    ) {
        super(apiStub, taskConfig, namespace);
        this.application = application;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Deleting application {} if exists", application.getInternalName());
        if (applicationExists()) {
            log.debug("Delete application {}", application.getInternalName());
            switch (application.getWorkloadType()) {
                case DEPLOYMENT ->
                    getApiStub().getAppsV1Api()
                        .deleteNamespacedDeployment(application.getInternalName(), getNamespace()).execute();
                case STATEFUL_SET ->
                    getApiStub().getAppsV1Api()
                        .deleteNamespacedStatefulSet(application.getInternalName(), getNamespace()).execute();
                case DAEMON_SET ->
                    getApiStub().getAppsV1Api()
                        .deleteNamespacedDaemonSet(application.getInternalName(), getNamespace()).execute();
            }
        } else {
            log.debug("Skipping application {} deletion as it does not exist", application.getInternalName());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Application delete :: {} :: wait poll step", application.getInternalName());
        return !applicationExists();
    }

    private boolean applicationExists() throws ApiException {
        KubernetesListObject applications =
            switch (application.getWorkloadType()) {
                case DEPLOYMENT ->
                    getApiStub().getAppsV1Api().listNamespacedDeployment(getNamespace()).execute();
                case STATEFUL_SET ->
                    getApiStub().getAppsV1Api().listNamespacedStatefulSet(getNamespace()).execute();
                case DAEMON_SET ->
                    getApiStub().getAppsV1Api().listNamespacedDaemonSet(getNamespace()).execute();
            };
        return applications.getItems().stream()
            .anyMatch(
                app -> application.getInternalName().equals(app.getMetadata().getName()));
    }
}
