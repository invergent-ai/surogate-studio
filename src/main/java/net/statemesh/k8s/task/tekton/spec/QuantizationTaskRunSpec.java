package net.statemesh.k8s.task.tekton.spec;

import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.PullImageMode;
import net.statemesh.k8s.crd.tekton.models.*;
import net.statemesh.service.dto.TaskRunDTO;

import java.util.List;
import java.util.Map;

import static net.statemesh.k8s.util.K8SConstants.DENSEMAX_IMAGE;
import static net.statemesh.k8s.util.K8SConstants.TEKTON_JOB_NAME;

public class QuantizationTaskRunSpec extends V1TaskRunSpec implements TaskRunSpec {
    @Override
    public V1TaskRunSpec create(TaskRunDTO taskRun, ApplicationProperties applicationProperties) { // TODO
        return params(List.of())
            .taskSpec(new V1TaskSpec().steps(List.of(
                new V1TaskSpecStepsInner()
                    .name(TEKTON_JOB_NAME)
                    .image(DENSEMAX_IMAGE)
                    .imagePullPolicy(PullImageMode.PULL.getValue())
                    .command(List.of("quantize"))
            )))
            .computeResources(new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .requests(Map.of("cpu", "1", "memory", "4Gi"))
                .limits(Map.of("cpu", "4", "memory", "8Gi")));
    }
}
