package net.statemesh.k8s.task.control;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.extended.kubectl.KubectlScale;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.util.PatchUtils;
import net.statemesh.k8s.task.BaseTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.NodeDTO;
import net.statemesh.thread.VirtualWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static io.kubernetes.client.extended.kubectl.Kubectl.*;
import static net.statemesh.k8s.util.ApiUtils.replicas;
import static net.statemesh.k8s.util.K8SConstants.METADATA_RESTART_KEY;

public class ControlTask extends BaseTask<Void> {
    private final Logger log = LoggerFactory.getLogger(ControlTask.class);

    private final ControlObject controlObject;
    private final ControlCommand controlCommand;
    private final ApplicationDTO application;
    private final NodeDTO node;
    private final Integer replicas;

    public ControlTask(ApiStub apiStub,
                       TaskConfig taskConfig,
                       String namespace,
                       ControlObject controlObject,
                       ControlCommand controlCommand,
                       ApplicationDTO application,
                       NodeDTO node,
                       Integer replicas) {
        super(apiStub, taskConfig, namespace);
        this.controlObject = controlObject;
        this.controlCommand = controlCommand;
        this.application = application;
        this.node = node;
        this.replicas = replicas;
    }

    @Override
    public CompletableFuture<TaskResult<Void>> call() {
        deleteDanglingPods();

        switch (controlObject) {
            case NODE -> controlNode();
            case APPLICATION -> controlApplication();
        }

        boolean controlReady = VirtualWait.poll(
            Duration.ofSeconds(getTaskConfig().resourceOperationPollInterval()),
            Duration.ofSeconds(getTaskConfig().resourceOperationWaitTimeout()),
            () -> {
                try {
                    log.debug("## Control {} with name {} :: {} :: wait poll step",
                        controlObject,
                        ControlObject.APPLICATION.equals(controlObject) ?
                            application.getInternalName() : node.getInternalName(),
                        controlCommand);

                    if (ControlObject.APPLICATION.equals(controlObject)) {
                        final int readyReplicas = replicas(
                            getApiStub().getAppsV1Api(),
                            getNamespace(),
                            application.getInternalName(),
                            application.getWorkloadType()
                        ).readyReplicas();

                        return ControlCommand.STOP.equals(controlCommand) ? readyReplicas == 0 : readyReplicas > 0;
                    }

                    return true; // We don't check for node control commands finalization for now
                } catch (ApiException e) {
                    return false;
                }
            });
        log.info("{} {} control finished successfully [{}]", controlObject,
            ControlObject.APPLICATION.equals(controlObject) ? application.getName()
                : node.getInternalName(),
            controlReady);

        return CompletableFuture.completedFuture(
            TaskResult.<Void>builder()
                .success(controlReady)
                .waitTimeout(!controlReady)
                .build()
        );
    }

    private void controlNode() {
        try {
            switch (controlCommand) {
                case CORDON -> cordon().apiClient(getApiStub().getApiClient()).name(node.getInternalName()).execute();
                case UNCORDON -> uncordon().apiClient(getApiStub().getApiClient()).name(node.getInternalName()).execute();
                case DRAIN -> drain().apiClient(getApiStub().getApiClient()).name(node.getInternalName()).execute();
            }
        } catch (KubectlException e) {
            throw new RuntimeException(e);
        }
    }

    private void controlApplication() {
        KubectlScale<? extends KubernetesObject> scaler =
            scale(application.getWorkloadType().getClazz())
                .apiClient(getApiStub().getApiClient())
                .namespace(getNamespace())
                .name(application.getInternalName());

        try {
            switch (controlCommand) {
                case START -> scaler.replicas(application.getReplicas()).execute();
                case STOP -> scaler.replicas(0).execute();
                case RESTART -> {
                    scaler.replicas(0).execute();
                    scaler.replicas(application.getReplicas()).execute();
                }
                case SCALE -> scaler.replicas(replicas).execute();
                case RESTART_ROLLOUT -> rolloutRestart();
            }
        } catch (KubectlException e) {
            throw new RuntimeException(e);
        }
    }

    private void rolloutRestart() {
        try {
            PatchUtils.PatchCallFunc callFunc = switch (application.getWorkloadType()) {
                case DEPLOYMENT -> {
                    V1Deployment runningDeployment = getApiStub().getAppsV1Api()
                        .readNamespacedDeployment(application.getInternalName(), getNamespace()).execute();
                    runningDeployment
                        .getSpec()
                        .getTemplate()
                        .getMetadata()
                        .putAnnotationsItem(METADATA_RESTART_KEY, Instant.now().toString());
                    yield () -> getApiStub().getAppsV1Api().patchNamespacedDeployment(
                        application.getInternalName(),
                        getNamespace(),
                        new V1Patch(JSON.serialize(runningDeployment))).buildCall(null);
                }
                case STATEFUL_SET -> {
                    V1StatefulSet runningStatefulSet = getApiStub().getAppsV1Api()
                        .readNamespacedStatefulSet(application.getInternalName(), getNamespace()).execute();
                    runningStatefulSet
                        .getSpec()
                        .getTemplate()
                        .getMetadata()
                        .putAnnotationsItem(METADATA_RESTART_KEY, Instant.now().toString());
                    yield () -> getApiStub().getAppsV1Api().patchNamespacedStatefulSet(
                        application.getInternalName(),
                        getNamespace(),
                        new V1Patch(JSON.serialize(runningStatefulSet))).buildCall(null);
                }
                case DAEMON_SET -> {
                    V1DaemonSet runningDaemonSet = getApiStub().getAppsV1Api()
                        .readNamespacedDaemonSet(application.getInternalName(), getNamespace()).execute();
                    runningDaemonSet
                        .getSpec()
                        .getTemplate()
                        .getMetadata()
                        .putAnnotationsItem(METADATA_RESTART_KEY, Instant.now().toString());
                    yield () -> getApiStub().getAppsV1Api().patchNamespacedDaemonSet(
                        application.getInternalName(),
                        getNamespace(),
                        new V1Patch(JSON.serialize(runningDaemonSet))).buildCall(null);
                }
            };

            PatchUtils.patch(
                application.getWorkloadType().getClazz(),
                callFunc,
                V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH,
                getApiStub().getApiClient()
            );
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public enum ControlCommand {
        START,
        STOP,
        RESTART,
        RESTART_ROLLOUT,
        SCALE,
        CORDON, // Accept no more containers
        UNCORDON,
        DRAIN // Remove all containers
    }

    public enum ControlObject {
        NODE,
        APPLICATION,
    }
}
