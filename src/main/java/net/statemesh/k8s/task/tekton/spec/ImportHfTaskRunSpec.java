package net.statemesh.k8s.task.tekton.spec;

import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.PullImageMode;
import net.statemesh.k8s.crd.tekton.models.*;
import net.statemesh.service.dto.TaskRunDTO;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.statemesh.k8s.util.K8SConstants.TEKTON_JOB_NAME;

public class ImportHfTaskRunSpec extends V1TaskRunSpec implements TaskRunSpec {
    @Override
    public V1TaskRunSpec create(TaskRunDTO taskRun, ApplicationProperties applicationProperties) {
        return new V1TaskRunSpec()
            .params(toParams(taskRun, applicationProperties))
            .taskSpec(new V1TaskSpec().steps(List.of(new V1TaskSpecStepsInner()
                .name(TEKTON_JOB_NAME)
                .image("docker.io/statemesh/hf-api:1.1") // TODO - Change to DENSEMAX_IMAGE
                .imagePullPolicy(PullImageMode.PULL.getValue())
                .command(List.of("python3"))
                .args(List.of("-m", importScript(taskRun)))
                .env(envVars(taskRun,
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
            .computeResources(computeResources(taskRun));
    }

    private String importScript(TaskRunDTO taskRun) {
        return switch (taskRun.getType()) {
            case IMPORT_HF_MODEL -> "densemax.jobs.download_hf_model";
            case IMPORT_HF_DATASET -> "densemax.jobs.download_hf_dataset";
            default -> throw new NotImplementedException("Unexpected value: " + taskRun.getType());
        };
    }

    private List<V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner> envVars(TaskRunDTO taskRun,
                                                                                V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner... common) {
        var commonEnv = new ArrayList<>(List.of(common));
        switch (taskRun.getType()) {
            case IMPORT_HF_MODEL -> commonEnv.add(
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("HF_MODEL_ID").value("$(params.hf-repo-id)"));
            case IMPORT_HF_DATASET -> commonEnv.addAll(List.of(
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("HF_DATASET_ID").value("$(params.hf-repo-id)"),
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("HF_DATASET_SUBSET").value("$(params.hf-subset)")));
            default -> throw new NotImplementedException("Unexpected value: " + taskRun.getType());
        }
        return commonEnv;
    }

    private V1PipelineRunSpecTaskRunSpecsInnerComputeResources computeResources(TaskRunDTO taskRun) {
        return switch (taskRun.getType()) {
            case IMPORT_HF_MODEL -> new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .requests(Map.of("cpu", "8", "memory", "32Gi"))
                .limits(Map.of("cpu", "8", "memory", "32Gi"));
            case IMPORT_HF_DATASET -> new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .limits(Map.of("cpu", "4", "memory", "16Gi"));
            default -> throw new NotImplementedException("Unexpected value: " + taskRun.getType());
        };
    }
}
