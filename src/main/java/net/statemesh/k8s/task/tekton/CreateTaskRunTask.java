package net.statemesh.k8s.task.tekton;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.options.CreateOptions;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.TaskRunType;
import net.statemesh.k8s.crd.tekton.models.V1TaskRun;
import net.statemesh.k8s.crd.tekton.models.V1TaskRunSpec;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.task.tekton.spec.EvaluationTaskRunSpec;
import net.statemesh.k8s.task.tekton.spec.ImportHfTaskRunSpec;
import net.statemesh.k8s.task.tekton.spec.QuantizationTaskRunSpec;
import net.statemesh.k8s.task.tekton.spec.SkyTaskRunSpec;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.repository.ApplicationRepository;
import net.statemesh.service.dto.TaskRunDTO;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static net.statemesh.k8s.util.K8SConstants.*;

@Slf4j
public class CreateTaskRunTask extends BaseMutationTask<String> {
    private final TaskRunDTO taskRun;
    private final ApplicationProperties applicationProperties;
    private final ApplicationRepository applicationRepository;

    public CreateTaskRunTask(ApiStub apiStub,
                             TaskConfig taskConfig,
                             String namespace,
                             TaskRunDTO taskRun,
                             ApplicationProperties applicationProperties,
                             ApplicationRepository applicationRepository) {
        super(apiStub, taskConfig, namespace);
        this.taskRun = taskRun;
        this.applicationProperties = applicationProperties;
        this.applicationRepository = applicationRepository;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<String> taskResult) throws Exception {
        log.info("Running Task...");
        if (taskRunExists()) {
            log.debug("Skipping task run {} creation as it exists", taskRun.getName());
            throw new SkippedExistsException();
        }
        log.debug("Create task run {}", taskRun.getName());

        var response = getApiStub().getTaskRun().create(
            getNamespace(),
            new V1TaskRun()
                .apiVersion(TEKTON_GROUP + "/" + TEKTON_API_VERSION)
                .kind(TEKTON_TASK_RUN_KIND)
                .metadata(new V1ObjectMeta()
                    .annotations(sky() ? Map.of(NAD_SELECTOR_ANNOTATION, SRIOV_NAD_NAME) : Collections.emptyMap())
                    .name(taskRun.getInternalName())
                    .namespace(getNamespace()))
                .spec(taskRunSpec()),
            new CreateOptions()
        ).throwsApiException();

        taskResult.value(Objects.requireNonNull(response.getObject().getMetadata()).getUid());
    }

    private V1TaskRunSpec taskRunSpec() {
        return switch (taskRun.getType()) {
            case EVALUATION -> new EvaluationTaskRunSpec(applicationRepository).create(taskRun, applicationProperties);
            case IMPORT_HF_MODEL, IMPORT_HF_DATASET -> new ImportHfTaskRunSpec().create(taskRun, applicationProperties);
            case QUANTIZATION -> new QuantizationTaskRunSpec().create(taskRun, applicationProperties);
            case TRAIN, FINE_TUNE -> new SkyTaskRunSpec().create(taskRun, applicationProperties);
        };
    }

    private boolean sky() {
        return TaskRunType.TRAIN.equals(taskRun.getType()) || TaskRunType.FINE_TUNE.equals(taskRun.getType());
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
