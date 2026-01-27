package net.statemesh.k8s.task.tekton;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.options.CreateOptions;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.crd.tekton.models.V1TaskRun;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.domain.enumeration.TaskRunType;
import net.statemesh.service.dto.TaskRunDTO;
import net.statemesh.service.dto.TaskRunParamDTO;

import java.util.Objects;

import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.serviceName;

@Slf4j
public class CreateTaskRunTask extends BaseMutationTask<String> {
    private final TaskRunDTO taskRun;
    private final TaskSpecs taskSpecs;

    public CreateTaskRunTask(ApiStub apiStub,
                             TaskConfig taskConfig,
                             String namespace,
                             TaskRunDTO taskRun,
                             TaskSpecs taskSpecs) {
        super(apiStub, taskConfig, namespace);
        this.taskRun = taskRun;
        this.taskSpecs = taskSpecs;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<String> taskResult) throws Exception {
        log.info("Running Task...");

        if (taskRunExists()) {
            log.debug("Skipping task run {} creation as it exists", taskRun.getName());
            throw new SkippedExistsException();
        }

        if (taskRun.getType() == TaskRunType.EVALUATION) {
            resolveAndOverrideJudgeEndpoint();
            resolveAndOverrideSimulatorEndpoint();
        }

        log.debug("Create task run {}", taskRun.getName());

        String modelEndpoint = resolveModelEndpoint();
        String ingressEndpoint = resolveIngressEndpoint();

        var response = getApiStub().getTaskRun().create(
            getNamespace(),
            new V1TaskRun()
                .apiVersion(TEKTON_GROUP + "/" + TEKTON_API_VERSION)
                .kind(TEKTON_TASK_RUN_KIND)
                .metadata(new V1ObjectMeta()
                    .name(taskRun.getInternalName())
                    .namespace(getNamespace()))
                .spec(this.taskSpecs.createTaskSpec(this.taskRun, modelEndpoint, ingressEndpoint)),
            new CreateOptions()
        ).throwsApiException();

        taskResult.value(Objects.requireNonNull(response.getObject().getMetadata()).getUid());
    }

    private String resolveInternalModelEndpoint(String internalName, String namespace) {
        if (internalName == null || namespace == null) {
            return null;
        }

        String endpoint = String.format("http://%s/v1", serviceName(internalName, "80"));
        log.info("Resolved internal model endpoint: {}", endpoint);
        return endpoint;
    }

    private void resolveAndOverrideJudgeEndpoint() {
        String judgeEndpoint = resolveInternalModelEndpoint(
            getParamValue("JUDGE_MODEL_INTERNAL_NAME"),
            getParamValue("JUDGE_MODEL_NAMESPACE")
        );
        if (judgeEndpoint != null) {
            taskRun.getParams().removeIf(p -> "JUDGE_MODEL_BASE_URL".equals(p.getKey()));
            taskRun.getParams().add(TaskRunParamDTO.builder()
                .key("JUDGE_MODEL_BASE_URL")
                .value(judgeEndpoint)
                .build());
        }
    }

    private void resolveAndOverrideSimulatorEndpoint() {
        String simulatorEndpoint = resolveInternalModelEndpoint(
            getParamValue("SIMULATOR_MODEL_INTERNAL_NAME"),
            getParamValue("SIMULATOR_MODEL_NAMESPACE")
        );
        if (simulatorEndpoint != null) {
            taskRun.getParams().removeIf(p -> "SIMULATOR_MODEL_BASE_URL".equals(p.getKey()));
            taskRun.getParams().add(TaskRunParamDTO.builder()
                .key("SIMULATOR_MODEL_BASE_URL")
                .value(simulatorEndpoint)
                .build());
        }
    }



    private String resolveIngressEndpoint() {
        if (taskRun.getType() != TaskRunType.EVALUATION) {
            return null;
        }

        String ingressHostName = getParamValue("INGRESS_HOST_NAME");
        if (ingressHostName != null) {
            return "https://" + ingressHostName + "/v1";
        }
        return null;
    }

    private String resolveModelEndpoint() {
        if (taskRun.getType() != TaskRunType.EVALUATION) {
            return null;
        }

        // Use internal name for service discovery, not the model name
        String deployedModelInternalName = getParamValue("DEPLOYED_MODEL_INTERNAL_NAME");
        String useGateway = getParamValue("USE_GATEWAY");

        if (deployedModelInternalName == null) {
            log.warn("DEPLOYED_MODEL_INTERNAL_NAME not found in task params");
            return null;
        }

        String serviceLabel = "true".equals(useGateway)
            ? deployedModelInternalName + "-router"
            : deployedModelInternalName + "-worker";

        String labelSelector = "app.kubernetes.io/instance=" + serviceLabel;

        try {
            var services = getApiStub().getCoreV1Api()
                .listNamespacedService(getNamespace())
                .labelSelector(labelSelector)
                .execute();

            for (var svc : services.getItems()) {
                if (svc.getSpec() == null || svc.getSpec().getPorts() == null) {
                    continue;
                }
                for (var port : svc.getSpec().getPorts()) {
                    if (port.getPort() == 80) {
                        String endpoint = String.format("http://%s:%d/v1",
                            svc.getMetadata().getName(),
                            port.getPort());
                        log.info("Resolved model endpoint: {}", endpoint);
                        return endpoint;
                    }
                }
            }
            log.warn("No service with port 80 found for label {}", labelSelector);
        } catch (ApiException e) {
            log.error("Failed to find service with label {}", labelSelector, e);
        }

        return null;
    }

    private String getParamValue(String key) {
        return taskRun.getParams().stream()
            .filter(p -> key.equals(p.getKey()))
            .map(TaskRunParamDTO::getValue)
            .findFirst()
            .orElse(null);
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Tekton task run :: {} :: wait poll step", taskRun.getName());
        return taskRunExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<String> taskResult, boolean ready) {
        log.info("Tekton task {} created successfully [{}]", taskRun.getName(), ready);
    }

    protected boolean taskRunExists() {
        var taskRuns = getApiStub().getTaskRun().list(getNamespace()).getObject();
        return taskRuns != null && taskRuns.getItems().stream()
            .anyMatch(t -> taskRun.getInternalName().equals(
                Objects.requireNonNull(t.getMetadata()).getName())
            );
    }
}
