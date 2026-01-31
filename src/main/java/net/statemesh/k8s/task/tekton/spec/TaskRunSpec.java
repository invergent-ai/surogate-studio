package net.statemesh.k8s.task.tekton.spec;

import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.crd.tekton.models.V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner;
import net.statemesh.k8s.crd.tekton.models.V1TaskRunSpec;
import net.statemesh.service.dto.TaskRunDTO;
import net.statemesh.service.dto.TaskRunParamDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface TaskRunSpec {
    V1TaskRunSpec create(TaskRunDTO taskRun, ApplicationProperties applicationProperties);

    default List<V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner> toParams(TaskRunDTO taskRun,
                                                                                   ApplicationProperties applicationProperties) {
        return toTaskRunParams(taskRun,
            new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                .name("lakefs-endpoint").value(applicationProperties.getLakeFs().getEndpointInternal() + "/api/v1"),
            new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                .name("lakefs-s3-endpoint").value(applicationProperties.getLakeFs().getS3EndpointInternal()),
            new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                .name("lakefs-access-key").value(applicationProperties.getLakeFs().getKey()),
            new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                .name("lakefs-secret-key").value(applicationProperties.getLakeFs().getSecret())
        );
    }

    default List<V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner> toTaskRunParams(TaskRunDTO taskRun,
                                                                                          V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner... params) {
        var mappedParams = taskRun.getParams().stream().
            map(p -> new V1PipelineSpecFinallyInnerMatrixIncludeInnerParamsInner()
                .name(p.getKey())
                .value(p.getValue()))
            .toList();
        var addonParams = Arrays.asList(params);
        var finalParams = new ArrayList<>(mappedParams);
        finalParams.addAll(addonParams);
        return finalParams;
    }

    default String getParamValue(TaskRunDTO taskRun, String key) {
        return taskRun.getParams().stream()
            .filter(p -> key.equals(p.getKey()))
            .map(TaskRunParamDTO::getValue)
            .findFirst()
            .orElse(null);
    }

    default boolean hasParam(TaskRunDTO taskRun, String key) {
        return taskRun.getParams().stream()
            .anyMatch(p -> key.equals(p.getKey()) && p.getValue() != null && !p.getValue().isEmpty());
    }
}
