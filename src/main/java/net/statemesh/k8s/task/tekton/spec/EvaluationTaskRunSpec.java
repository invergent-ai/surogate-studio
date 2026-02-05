package net.statemesh.k8s.task.tekton.spec;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.PullImageMode;
import net.statemesh.k8s.crd.tekton.models.*;
import net.statemesh.service.dto.TaskRunDTO;
import net.statemesh.service.dto.TaskRunParamDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static net.statemesh.k8s.util.K8SConstants.DENSEMAX_IMAGE;
import static net.statemesh.k8s.util.K8SConstants.TEKTON_JOB_NAME;
import static net.statemesh.k8s.util.NamingUtils.serviceName;

@Slf4j
public class EvaluationTaskRunSpec extends V1TaskRunSpec implements TaskRunSpec {
    @Override
    public V1TaskRunSpec create(TaskRunDTO taskRun, ApplicationProperties applicationProperties) {
        resolveAndOverrideJudgeEndpoint(taskRun);
        resolveAndOverrideSimulatorEndpoint(taskRun);

        return new V1TaskRunSpec()
            .params(toParams(taskRun, applicationProperties))
            .taskSpec(new V1TaskSpec().steps(List.of(
                new V1TaskSpecStepsInner()
                    .name(TEKTON_JOB_NAME)
                    .image(DENSEMAX_IMAGE)
                    .imagePullPolicy(PullImageMode.PULL.getValue())
                    .command(List.of("eval"))
                    .env(envVars(taskRun))
                    .volumeMounts(List.of(
                        new V1TaskSpecSidecarsInnerVolumeMountsInner()
                            .name("docker-socket")
                            .mountPath("/var/run/docker.sock")
                    ))
            )))
            .podTemplate(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplate()
                .volumes(volumes())
            )
            .computeResources(new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .requests(Map.of("cpu", "2", "memory", "8Gi"))
                .limits(Map.of("cpu", "8", "memory", "32Gi")));
    }

    private List<Map<String, Object>> volumes() {
        // Create volume definition for Docker socket (volumes is Object type, so use Map)
        Map<String, Object> dockerSocketVolume = new HashMap<>();
        dockerSocketVolume.put("name", "docker-socket");
        Map<String, Object> hostPath = new HashMap<>();
        hostPath.put("path", "/var/run/docker.sock");
        hostPath.put("type", "Socket");
        dockerSocketVolume.put("hostPath", hostPath);
        return List.of(dockerSocketVolume);
    }

    private List<V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner> envVars(TaskRunDTO taskRun) {
        List<V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner> envVars = new ArrayList<>(
            List.of(
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("RCLONE_CONFIG_LAKEFS_ENDPOINT").value("$(params.lakefs-s3-endpoint)"),
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("LAKEFS_ENDPOINT").value("$(params.lakefs-endpoint)"),
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("LAKEFS_KEY").value("$(params.lakefs-access-key)"),
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("LAKEFS_SECRET").value("$(params.lakefs-secret-key)"),
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("EVAL_JOB_ID").value(taskRun.getId()),
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("DEPLOYED_MODEL_NAME").value("$(params.DEPLOYED_MODEL_NAME)"),
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("DEPLOYED_MODEL_NAMESPACE").value("$(params.DEPLOYED_MODEL_NAMESPACE)"),
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("USE_GATEWAY").value("$(params.USE_GATEWAY)"),
                new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name("LANGUAGE").value("$(params.LANGUAGE)")
            )
        );

        envVars.addAll(
            Stream.of("SECURITY_TESTS", "CUSTOM_EVAL_DATASETS", "RED_TEAMING_CONFIG", "MODEL_TOKENIZER",
                    "MODEL_TEMPERATURE", "MODEL_TOP_P", "MODEL_TOP_K", "MODEL_MIN_P",
                    "MODEL_PRESENCE_PENALTY", "MODEL_ENABLE_THINKING", "CUSTOM_EVAL_DATASETS", "JUDGE_MODEL",
                    "JUDGE_MODEL_API", "JUDGE_MODEL_BASE_URL", "SIMULATOR_MODEL", "SIMULATOR_MODEL_API",
                    "SIMULATOR_MODEL_BASE_URL", "QUALITY_METRICS", "CONVERSATION_METRICS", "PERFORMANCE_METRICS",
                    "JUDGE_MODEL_PROVIDER", "SIMULATOR_MODEL_PROVIDER", "JOB_NAME", "JOB_DESCRIPTION", "BENCHMARKS")
                .filter(param -> hasParam(taskRun, param))
                .map(param -> new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                    .name(param).value("$(params." + param + ")")
                )
                .toList()
        );

        // Model endpoints - internal (preferred) and ingress (fallback)
        String modelEndpoint = resolveInternalModelEndpoint(getParamValue(taskRun, "DEPLOYED_MODEL_INTERNAL_NAME"));
        if (modelEndpoint != null) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("MODEL_ENDPOINT").value(modelEndpoint));
        }
        String ingressEndpoint = resolveIngressEndpoint(taskRun);
        if (ingressEndpoint != null) {
            envVars.add(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                .name("MODEL_ENDPOINT_FALLBACK").value(ingressEndpoint));
        }

        return envVars;
    }

    private void resolveAndOverrideJudgeEndpoint(TaskRunDTO taskRun) {
        String judgeEndpoint = resolveInternalModelEndpoint(getParamValue(taskRun, "JUDGE_MODEL_INTERNAL_NAME"));
        if (judgeEndpoint != null) {
            taskRun.getParams().removeIf(p -> "JUDGE_MODEL_BASE_URL".equals(p.getKey()));
            taskRun.getParams().add(TaskRunParamDTO.builder()
                .key("JUDGE_MODEL_BASE_URL")
                .value(judgeEndpoint)
                .build());
        }
    }

    private void resolveAndOverrideSimulatorEndpoint(TaskRunDTO taskRun) {
        String simulatorEndpoint = resolveInternalModelEndpoint(getParamValue(taskRun, "SIMULATOR_MODEL_INTERNAL_NAME"));
        if (simulatorEndpoint != null) {
            taskRun.getParams().removeIf(p -> "SIMULATOR_MODEL_BASE_URL".equals(p.getKey()));
            taskRun.getParams().add(TaskRunParamDTO.builder()
                .key("SIMULATOR_MODEL_BASE_URL")
                .value(simulatorEndpoint)
                .build());
        }
    }

    private String resolveIngressEndpoint(TaskRunDTO taskRun) {
        String ingressHostName = getParamValue(taskRun, "INGRESS_HOST_NAME");
        if (ingressHostName != null) {
            return "https://" + ingressHostName + "/v1";
        }
        return null;
    }

    private String resolveInternalModelEndpoint(String internalName) {
        if (internalName == null) {
            return null;
        }

        // TODO - Test and fix if portName is ok for the three scenarios: modelEndpoint,judgeEndpoint,simulatorEndpoint. If not ok bring it as param
        String endpoint = String.format("http://%s/v1", serviceName(internalName, "80"));
        log.info("Resolved internal model endpoint: {}", endpoint);
        return endpoint;
    }
}
