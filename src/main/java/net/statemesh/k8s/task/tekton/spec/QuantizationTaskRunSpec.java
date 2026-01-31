package net.statemesh.k8s.task.tekton.spec;

import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.crd.tekton.models.V1PipelineRunSpecTaskRunSpecsInnerComputeResources;
import net.statemesh.k8s.crd.tekton.models.V1TaskRunSpec;
import net.statemesh.k8s.crd.tekton.models.V1TaskSpec;
import net.statemesh.service.dto.TaskRunDTO;

import java.util.List;
import java.util.Map;

public class QuantizationTaskRunSpec extends V1TaskRunSpec implements TaskRunSpec {
    @Override
    public V1TaskRunSpec create(TaskRunDTO taskRun, ApplicationProperties applicationProperties) { // TODO
        return params(List.of())
            .taskSpec(new V1TaskSpec())
            .computeResources(new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .requests(Map.of("cpu", "1", "memory", "4Gi"))
                .limits(Map.of("cpu", "4", "memory", "8Gi")));
    }
}
