package net.statemesh.service.util;

import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.TaskRunType;
import net.statemesh.service.dto.*;
import org.apache.logging.log4j.util.Strings;

import java.util.*;
import java.util.stream.Collectors;

import static net.statemesh.config.Constants.*;
import static net.statemesh.k8s.util.K8SConstants.*;

public class SkyUtil {
    public static SkyConfigDTO setupSkyConfig(SkyConfigDTO skyConfig, RayJobDTO rayJob) {
        return skyConfig
            .withImageId(
                    Optional.ofNullable(rayJob.getSkyToK8s()).orElse(Boolean.TRUE) ? SUROGATE_IMAGE :
                            USE_AXOLOTL_TRAINING_LIBRARY ?
                                    SUROGATE_TRAIN_AXOLOTL_IMAGE :
                                    SUROGATE_TRAIN_SUROGATE_IMAGE
            )
            .withSetup(String.format("""
                echo "Setup runtime environment for model ${BASE_MODEL} training using %s library"
                """,
                USE_AXOLOTL_TRAINING_LIBRARY ? "Axolotl" : "Surogate"
            ))
            .withRun(String.format("""
                cd /opt/densemax/%s
                source .venv/bin/activate
                ~/sky_templates/ray/start_cluster

                if [ "$SKYPILOT_NODE_RANK" == "0" ]; then
                    job-entry-sky %s %s
                fi
                """,
                USE_AXOLOTL_TRAINING_LIBRARY ? "train-axolotl" : "train",
                USE_AXOLOTL_TRAINING_LIBRARY ? "train-axolotl" : "train",
                USE_AXOLOTL_TRAINING_LIBRARY ? "train-sky-axolotl" : "train-sky"
            ))
            .withConfig(
                Optional.ofNullable(rayJob.getSkyToK8s()).orElse(Boolean.FALSE) ?
                    densemaxK8sConfig() : Collections.emptyMap()
            )
            .withEnvs(envs(rayJob));
    }

    public static TaskRunDTO rayJobToSkyTaskRun(RayJobDTO rayJob, ApplicationProperties applicationProperties) {
        return TaskRunDTO.builder()
                .name(rayJob.getName())
                .internalName(rayJob.getInternalName())
                .project(rayJob.getProject())
                .deployedNamespace(rayJob.getDeployedNamespace())
                .workDirVolumeName(rayJob.getWorkDirVolumeName())
                .skyToK8s(rayJob.getSkyToK8s())
                .type(Optional.of(rayJob.getType()).map(type ->
                    switch (type) {
                        case TRAIN -> TaskRunType.TRAIN;
                        case FINE_TUNE -> TaskRunType.FINE_TUNE;
                    }).orElseThrow(() -> new RuntimeException("Job type was not present"))
                )
                .params(taskRunParams(rayJob, applicationProperties))
                .build();
    }

    private static Set<TaskRunParamDTO> taskRunParams(RayJobDTO rayJob, ApplicationProperties applicationProperties) {
        // RayJob params (from FE) - includes cloud keys
        var jobParams = rayJob.getEnvVars().stream()
            .map(envVar ->
                TaskRunParamDTO.builder()
                    .key(envVar.getKey())
                    .value(envVar.getValue())
                    .build()
            ).collect(Collectors.toSet());
        // ID params
        jobParams.add(
            TaskRunParamDTO.builder()
                .key(RAY_JOB_ENV_JOB_ID)
                .value(rayJob.getInternalName())
                .build()
        );
        jobParams.add(
            TaskRunParamDTO.builder()
                .key(RAY_JOB_ENV_WORK_DIR)
                .value(RAY_WORK_DIR)
                .build()
        );
        // LakeFS connection params
        jobParams.add(
            TaskRunParamDTO.builder()
                .key(RAY_JOB_ENV_LAKECTL_SERVER_ENDPOINT_URL)
                .value(applicationProperties.getLakeFs().getEndpointInternal())
                .build()
        );
        jobParams.add(
            TaskRunParamDTO.builder()
                .key(RAY_JOB_ENV_LAKECTL_CREDENTIALS_ACCESS_KEY_ID)
                .value(applicationProperties.getLakeFs().getKey())
                .build()
        );
        jobParams.add(
            TaskRunParamDTO.builder()
                .key(RAY_JOB_ENV_LAKECTL_CREDENTIALS_SECRET_ACCESS_KEY)
                .value(applicationProperties.getLakeFs().getSecret())
                .build()
        );
        // Sky params
        jobParams.add(
            TaskRunParamDTO.builder()
                .key(TASK_RUN_ENV_KUBE_CONFIG)
                .value(rayJob.getProject().getCluster().getKubeConfig())
                .build()
        );
        jobParams.add(
            TaskRunParamDTO.builder()
                .key(TASK_RUN_ENV_SKY_CONFIG)
                .value(rayJob.getSkyConfig())
                .build()
        );
        jobParams.add(
            TaskRunParamDTO.builder()
                .key(TASK_RUN_ENV_USE_AXOLOTL)
                .value(USE_AXOLOTL_TRAINING_LIBRARY.toString())
                .build()
        );
        if (USE_SKYPILOT_SERVER) {
            jobParams.add(
                TaskRunParamDTO.builder()
                    .key(TASK_RUN_ENV_SKYPILOT_ENDPOINT)
                    .value(applicationProperties.getSkypilotServerUrl())
                    .build()
            );
        }

        return jobParams;
    }

    private static Map<String, String> envs(RayJobDTO rayJob) {
        var baseModel = rayJob.getEnvVars()
            .stream().filter(envVar -> RAY_JOB_ENV_BASE_MODEL.equals(envVar.getKey()))
            .map(JobEnvironmentVariableDTO::getValue)
            .findAny().orElse(Strings.EMPTY);

        var envs = new HashMap<>(Map.of(
            RAY_JOB_ENV_JOB_ID, rayJob.getInternalName(),
            RAY_JOB_ENV_WORK_DIR, RAY_WORK_DIR,
            RAY_JOB_ENV_AXOLOTL_CONFIG, rayJob.getTrainingConfig(),
            RAY_JOB_ENV_BASE_MODEL, baseModel));
        if (Optional.ofNullable(rayJob.getSkyToK8s()).orElse(Boolean.FALSE)) {
            envs.putAll(densemaxK8sEnvs());
        }

        return envs;
    }

    private static Map<String, String> densemaxK8sEnvs() {
        var envs = new HashMap<String, String>();
        envs.put("NVIDIA_VISIBLE_DEVICES", "all");
        envs.put("NCCL_IB_DISABLE", "0");
        envs.put("NCCL_P2P_DISABLE", "1");
        envs.put("NCCL_SHM_DISABLE", "0");
        envs.put("NCCL_DEBUG", "info");
        envs.put("NCCL_SOCKET_IFNAME", "net1");
        envs.put("NCCL_IB_PORT", "1");
        envs.put("NCCL_DEBUG_SUBSYS", "INIT,NET,ENV");
        envs.put("NCCL_IB_HCA", "mlx5_1,mlx5_2,mlx5_3,mlx5_4,mlx5_5,mlx5_6,mlx5_7");

        return envs;
    }

    private static Map<String, Object> densemaxK8sConfig() {
        return Map.of(
            "kubernetes", Map.of(
                "pod_config", Map.of(
                    "metadata", Map.of(
                        "annotations", Map.of(
                            NAD_SELECTOR_ANNOTATION,
                            SRIOV_NAD_NAME
                        )
                    ),
                    "spec", Map.of(
                        "dnsConfig", Map.of(
                            "options", List.of(
                                Map.of(
                                    "name", "ndots",
                                    "value", "1"
                                )
                            )
                        ),
                        "containers", List.of(
                            Map.of(
                                "name", "ray-node",
                                "securityContext", Map.of(
                                    "capabilities", Map.of(
                                        "add", List.of("IPC_LOCK")
                                    )
                                ),
                                "resources", Map.of(
                                    "requests", Map.of(
                                        SRIOV_RESOURCE_NAME, "1"
                                    ),
                                    "limits", Map.of(
                                        SRIOV_RESOURCE_NAME, "1"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );
    }
}
