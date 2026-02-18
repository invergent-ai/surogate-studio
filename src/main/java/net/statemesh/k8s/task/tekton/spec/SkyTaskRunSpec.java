package net.statemesh.k8s.task.tekton.spec;

import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.PullImageMode;
import net.statemesh.k8s.crd.tekton.models.*;
import net.statemesh.service.dto.TaskRunDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.pvcName;

public class SkyTaskRunSpec extends V1TaskRunSpec implements TaskRunSpec {
    @Override
    public V1TaskRunSpec create(TaskRunDTO taskRun, ApplicationProperties applicationProperties) {
        return new V1TaskRunSpec()
            .params(toParams(taskRun, applicationProperties))
            .taskSpec(new V1TaskSpec().steps(List.of(new V1TaskSpecStepsInner()
                .name(TEKTON_JOB_NAME)
                .image(DENSEMAX_IMAGE)
                .imagePullPolicy(PullImageMode.PULL.getValue())
                .command(List.of("launch-sky"))
                .env(
                    taskRun.getParams().stream()
                        .map(param -> new V1PipelineRunSpecTaskRunSpecsInnerPodTemplateEnvInner()
                            .name(param.getKey())
                            .value("$(params." + param.getKey() + ")"))
                        .toList()
                )
                .volumeMounts(volumeMounts())
            )))
            .timeout(SKY_TASK_TIMEOUT)
            .podTemplate(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplate()
                .volumes(volumes(taskRun))
            )
            .computeResources(new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .requests(
                    Map.of(
                        "cpu", "2",
                        "memory", "8Gi",
                        GPU_RESOURCE_NAME, taskRun.getRayClusterShape().getHeadGpus().toString(),
                        SRIOV_RESOURCE_NAME, "1"
                    )
                )
                .limits(
                    Map.of(
                        GPU_RESOURCE_NAME, taskRun.getRayClusterShape().getHeadGpus().toString(),
                        SRIOV_RESOURCE_NAME, "1"
                    )
                )
            );
    }

    private Object volumes(TaskRunDTO taskRun) {
        var volumes = new ArrayList<>(List.of(Map.of(
                        "name", "work-dir",
                        "persistentVolumeClaim", Map.of(
                                "claimName", pvcName(taskRun.getWorkDirVolumeName()))),
                Map.of(
                        "name", "aim",
                        "nfs", Map.of(
                                "server", NFS_SERVER,
                                "path", NFS_AIM_PATH
                        )
                )
        ));
        volumes.addAll(List.of(
            Map.of(
                    "name", "dshm",
                    "hostPath", Map.of(
                            "path", "/dev/shm")
            ),
            Map.of(
                    "name", "devinf",
                    "hostPath", Map.of(
                            "path", "/dev/infiniband")
            )
        ));
        return volumes;
    }

    private List<V1TaskSpecSidecarsInnerVolumeMountsInner> volumeMounts() {
        var volumeMounts = new ArrayList<>(List.of(
                new V1TaskSpecSidecarsInnerVolumeMountsInner()
                        .name("work-dir")
                        .mountPath(RAY_WORK_DIR),
                new V1TaskSpecSidecarsInnerVolumeMountsInner()
                        .name("aim")
                        .mountPath(AIM_DIR)
        ));
        volumeMounts.addAll(List.of(
            new V1TaskSpecSidecarsInnerVolumeMountsInner()
                .name("dshm")
                .mountPath("/dev/shm"),
            new V1TaskSpecSidecarsInnerVolumeMountsInner()
                .name("devinf")
                .mountPath("/dev/infiniband")
        ));

        return volumeMounts;
    }
}
