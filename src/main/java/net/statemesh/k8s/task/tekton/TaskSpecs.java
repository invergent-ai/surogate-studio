package net.statemesh.k8s.task.tekton;

import lombok.RequiredArgsConstructor;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.crd.tekton.models.*;
import net.statemesh.service.dto.TaskRunDTO;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskSpecs {
    private final ApplicationProperties applicationProperties;

    /**
     * Original method - backward compatible for non-evaluation tasks
     */
    public V1TaskRunSpec createTaskSpec(TaskRunDTO task) {
        return createTaskSpec(task, null, null);
    }

    /**
     * New method with model endpoints for evaluation tasks
     */
    public V1TaskRunSpec createTaskSpec(TaskRunDTO task, String modelEndpoint, String ingressEndpoint) {
        return switch (task.getType()) {
            case QUANTIZATION -> createQuantizatonTaskSpec(task);
            case EVALUATION -> createEvaluationTaskSpec(task, modelEndpoint, ingressEndpoint);
            case IMPORT_HF_MODEL -> createHfImportModelTaskSpec(task);
            case IMPORT_HF_DATASET -> createHfImportDatasetTaskSpec(task);
            default -> throw new NotImplementedException("Unexpected value: " + task.getType());
        };
    }

    /**
     * Task params:
     * - lakefs-repo
     * - lakefs-branch
     * - hf-repo-id
     * - hf-subset
     * - hf-token (can be empty)
     */
    private V1TaskRunSpec createHfImportDatasetTaskSpec(TaskRunDTO task) {
        return new V1TaskRunSpec()
            .params(toTaskRunParams(task,
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-endpoint").value("http://lakefs.statemesh.svc/api/v1"),
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-s3-endpoint").value("http://lakefs-s3.statemesh.svc"),
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-access-key").value(applicationProperties.getLakeFs().getKey()),
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-secret-key").value(applicationProperties.getLakeFs().getSecret())
            ))
            .taskSpec(new V1TaskSpec().steps(List.of(new V1TaskSpecStepsInner()
                .name("run-job")
                .image("docker.io/statemesh/hf-api:1.1")
                .imagePullPolicy("Always")
                .command(List.of("python3"))
                .args(List.of("-m", "densemax.jobs.download_hf_dataset"))
                .env(List.of(
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("HF_DATASET_ID").value("$(params.hf-repo-id)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("HF_DATASET_SUBSET").value("$(params.hf-subset)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("HF_TOKEN").value("$(params.hf-token)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("LAKEFS_BRANCH").value("$(params.lakefs-branch)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("LAKEFS_ENDPOINT").value("$(params.lakefs-endpoint)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("LAKEFS_KEY").value("$(params.lakefs-access-key)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("LAKEFS_REPO_ID").value("$(params.lakefs-repo)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("LAKEFS_SECRET").value("$(params.lakefs-secret-key)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("RCLONE_CONFIG_LAKEFS_ACCESS_KEY_ID").value("$(params.lakefs-access-key)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("RCLONE_CONFIG_LAKEFS_SECRET_ACCESS_KEY").value("$(params.lakefs-secret-key)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("RCLONE_CONFIG_LAKEFS_ENDPOINT").value("$(params.lakefs-s3-endpoint)")
                ))
            )))
            .computeResources(new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .limits(Map.of("cpu", "4", "memory", "16Gi")));
    }

    /**
     * Task params:
     * - lakefs-repo
     * - lakefs-branch
     * - hf-repo-id
     * - hf-token (can be empty)
     */
    private V1TaskRunSpec createHfImportModelTaskSpec(TaskRunDTO task) {
        return new V1TaskRunSpec()
            .params(toTaskRunParams(task,
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-endpoint").value("http://lakefs.statemesh.svc/api/v1"),
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-s3-endpoint").value("http://lakefs-s3.statemesh.svc"),
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-access-key").value(applicationProperties.getLakeFs().getKey()),
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-secret-key").value(applicationProperties.getLakeFs().getSecret())
            ))
            .taskSpec(new V1TaskSpec().steps(List.of(new V1TaskSpecStepsInner()
                .name("run-job")
                .image("docker.io/statemesh/hf-api:1.0")
                .imagePullPolicy("Always")
                .command(List.of("python3"))
                .args(List.of("-m", "densemax.jobs.download_hf_model"))
                .env(List.of(
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("HF_MODEL_ID").value("$(params.hf-repo-id)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("HF_TOKEN").value("$(params.hf-token)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("LAKEFS_BRANCH").value("$(params.lakefs-branch)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("LAKEFS_ENDPOINT").value("$(params.lakefs-endpoint)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("LAKEFS_KEY").value("$(params.lakefs-access-key)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("LAKEFS_REPO_ID").value("$(params.lakefs-repo)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("LAKEFS_SECRET").value("$(params.lakefs-secret-key)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("RCLONE_CONFIG_LAKEFS_ACCESS_KEY_ID").value("$(params.lakefs-access-key)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("RCLONE_CONFIG_LAKEFS_SECRET_ACCESS_KEY").value("$(params.lakefs-secret-key)"),
                    new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                        .name("RCLONE_CONFIG_LAKEFS_ENDPOINT").value("$(params.lakefs-s3-endpoint)")
                ))
            )))
            .computeResources(new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .requests(Map.of("cpu", "8", "memory", "32Gi"))
                .limits(Map.of("cpu", "8", "memory", "32Gi")));
    }

    private V1TaskRunSpec createEvaluationTaskSpec(TaskRunDTO task, String modelEndpoint, String ingressEndpoint) {
        List<V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner> envVars = new ArrayList<>();

        // Always required - LakeFS config
        envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
            .name("RCLONE_CONFIG_LAKEFS_ENDPOINT").value("$(params.lakefs-s3-endpoint)"));
        envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
            .name("LAKEFS_ENDPOINT").value("$(params.lakefs-endpoint)"));
        envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
            .name("LAKEFS_KEY").value("$(params.lakefs-access-key)"));
        envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
            .name("LAKEFS_SECRET").value("$(params.lakefs-secret-key)"));
        envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
            .name("EVAL_JOB_ID").value(task.getId().toString()));
        if (hasParam(task, "SECURITY_TESTS")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("SECURITY_TESTS").value("$(params.SECURITY_TESTS)"));
        }
        if (hasParam(task, "CUSTOM_EVAL_DATASETS")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("CUSTOM_EVAL_DATASETS").value("$(params.CUSTOM_EVAL_DATASETS)"));
        }

        if (hasParam(task, "RED_TEAMING_CONFIG")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("RED_TEAMING_CONFIG").value("$(params.RED_TEAMING_CONFIG)"));
        }

        if (hasParam(task, "MODEL_TOKENIZER")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("MODEL_TOKENIZER").value("$(params.MODEL_TOKENIZER)"));
        }

        // Model endpoints - internal (preferred) and ingress (fallback)
        if (modelEndpoint != null) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("MODEL_ENDPOINT").value(modelEndpoint));
        }
        if (ingressEndpoint != null) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("MODEL_ENDPOINT_FALLBACK").value(ingressEndpoint));
        }

        if (hasParam(task, "CUSTOM_EVAL_DATASETS")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("CUSTOM_EVAL_DATASETS").value("$(params.CUSTOM_EVAL_DATASETS)"));
        }

        // Keep for logging/metadata
        envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
            .name("DEPLOYED_MODEL_NAME").value("$(params.DEPLOYED_MODEL_NAME)"));
        envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
            .name("DEPLOYED_MODEL_NAMESPACE").value("$(params.DEPLOYED_MODEL_NAMESPACE)"));
        envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
            .name("USE_GATEWAY").value("$(params.USE_GATEWAY)"));
        envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
            .name("LANGUAGE").value("$(params.LANGUAGE)"));

        // Job metadata
        if (hasParam(task, "JOB_NAME")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("JOB_NAME").value("$(params.JOB_NAME)"));
        }
        if (hasParam(task, "JOB_DESCRIPTION")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("JOB_DESCRIPTION").value("$(params.JOB_DESCRIPTION)"));
        }

        if (hasParam(task, "BENCHMARKS")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("BENCHMARKS").value("$(params.BENCHMARKS)"));
        }

        if (hasParam(task, "JUDGE_MODEL")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("JUDGE_MODEL").value("$(params.JUDGE_MODEL)"));
        }
        if (hasParam(task, "JUDGE_MODEL_API")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("JUDGE_MODEL_API").value("$(params.JUDGE_MODEL_API)"));
        }

        if (hasParam(task, "JUDGE_MODEL_BASE_URL")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("JUDGE_MODEL_BASE_URL").value("$(params.JUDGE_MODEL_BASE_URL)"));
        }
        if (hasParam(task, "SIMULATOR_MODEL")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("SIMULATOR_MODEL").value("$(params.SIMULATOR_MODEL)"));
        }
        if (hasParam(task, "SIMULATOR_MODEL_API")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("SIMULATOR_MODEL_API").value("$(params.SIMULATOR_MODEL_API)"));
        }
        if (hasParam(task, "SIMULATOR_MODEL_BASE_URL")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("SIMULATOR_MODEL_BASE_URL").value("$(params.SIMULATOR_MODEL_BASE_URL)"));
        }

        if (hasParam(task, "QUALITY_METRICS")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("QUALITY_METRICS").value("$(params.QUALITY_METRICS)"));
        }
        if (hasParam(task, "CONVERSATION_METRICS")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("CONVERSATION_METRICS").value("$(params.CONVERSATION_METRICS)"));
        }
        if (hasParam(task, "PERFORMANCE_METRICS")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("PERFORMANCE_METRICS").value("$(params.PERFORMANCE_METRICS)"));
        }

        if (hasParam(task, "JUDGE_MODEL_PROVIDER")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("JUDGE_MODEL_PROVIDER").value("$(params.JUDGE_MODEL_PROVIDER)"));
        }
        if (hasParam(task, "SIMULATOR_MODEL_PROVIDER")) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("SIMULATOR_MODEL_PROVIDER").value("$(params.SIMULATOR_MODEL_PROVIDER)"));
        }

        // Create step with Docker socket volume mount for code benchmark sandbox
        V1TaskSpecStepsInner evalStep = new V1TaskSpecStepsInner()
            .name("run-job")
            .image("registry.densemax.local/statemesh/densemax:1.0.0")
            .imagePullPolicy("Always")
            .command(List.of("/usr/bin/eval"))
            .env(envVars)
            .volumeMounts(List.of(
                new V1TaskSpecSidecarsInnerVolumeMountsInner()
                    .name("docker-socket")
                    .mountPath("/var/run/docker.sock")
            ));

        // Create volume definition for Docker socket (volumes is Object type, so use Map)
        List<Map<String, Object>> volumesList = new ArrayList<>();
        Map<String, Object> dockerSocketVolume = new HashMap<>();
        dockerSocketVolume.put("name", "docker-socket");
        Map<String, Object> hostPath = new HashMap<>();
        hostPath.put("path", "/var/run/docker.sock");
        hostPath.put("type", "Socket");
        dockerSocketVolume.put("hostPath", hostPath);
        volumesList.add(dockerSocketVolume);

        // Create podTemplate with volumes
        V1PipelineRunSpecTaskRunSpecsInnerPodTemplate podTemplate =
            new V1PipelineRunSpecTaskRunSpecsInnerPodTemplate()
                .volumes(volumesList);

        return new V1TaskRunSpec()
            .params(toTaskRunParams(task,
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-endpoint").value("http://lakefs.statemesh.svc/api/v1"),
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-s3-endpoint").value("http://lakefs-s3.statemesh.svc"),
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-access-key").value(applicationProperties.getLakeFs().getKey()),
                new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                    .name("lakefs-secret-key").value(applicationProperties.getLakeFs().getSecret())
            ))
            .podTemplate(podTemplate)
            .taskSpec(new V1TaskSpec().steps(List.of(evalStep)))
            .computeResources(new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .requests(Map.of("cpu", "2", "memory", "8Gi"))
                .limits(Map.of("cpu", "8", "memory", "32Gi")));
    }

    /**
     * Check if a param exists and has a non-empty value
     */
    private boolean hasParam(TaskRunDTO task, String key) {
        return task.getParams().stream()
            .anyMatch(p -> key.equals(p.getKey()) && p.getValue() != null && !p.getValue().isEmpty());
    }

    private V1TaskRunSpec createQuantizatonTaskSpec(TaskRunDTO task) {
        return new V1TaskRunSpec()
            .params(List.of())
            .taskSpec(new V1TaskSpec())
            .computeResources(new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .requests(Map.of("cpu", "1", "memory", "4Gi"))
                .limits(Map.of("cpu", "4", "memory", "8Gi")));
    }

    private List<V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner> toTaskRunParams(TaskRunDTO task, V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner... params) {
        var mappedParams = task.getParams().stream().map(p -> new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
            .name(p.getKey())
            .value(p.getValue())
        ).toList();
        var addonParams = Arrays.asList(params);
        var finalParams = new ArrayList<>(mappedParams);
        finalParams.addAll(addonParams);
        return finalParams;
    }
}
