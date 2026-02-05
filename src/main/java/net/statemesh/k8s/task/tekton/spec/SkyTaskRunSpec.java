package net.statemesh.k8s.task.tekton.spec;

import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.PullImageMode;
import net.statemesh.k8s.crd.tekton.models.*;
import net.statemesh.service.dto.TaskRunDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                .volumeMounts(List.of(
                    new V1TaskSpecSidecarsInnerVolumeMountsInner()
                        .name("work-dir")
                        .mountPath(RAY_WORK_DIR),
                    new V1TaskSpecSidecarsInnerVolumeMountsInner()
                        .name("aim")
                        .mountPath(AIM_DIR),
                    new V1TaskSpecSidecarsInnerVolumeMountsInner()
                        .name("dshm")
                        .mountPath("/dev/shm"),
                    new V1TaskSpecSidecarsInnerVolumeMountsInner()
                        .name("devinf")
                        .mountPath("/dev/infiniband")
                ))
            )))
            .podTemplate(new V1PipelineRunSpecTaskRunSpecsInnerPodTemplate()
                .volumes(List.of(Map.of(
                    "name", "work-dir",
                    "persistentVolumeClaim", Map.of(
                        "claimName", pvcName(taskRun.getWorkDirVolumeName()))),
                    Map.of(
                    "name", "aim",
                    "nfs", Map.of(
                        "server", NFS_SERVER,
                        "path", NFS_AIM_PATH
                        )
                    ),
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
                ))
            )
            .computeResources(new V1PipelineRunSpecTaskRunSpecsInnerComputeResources()
                .limits(Map.of("cpu", "4", "memory", "16Gi")));
    }

//    private void volumes() {
//        Optional.ofNullable(rayJob.getSkyToK8s()).orElse(Boolean.FALSE)
//    }
//
//    private void volumeMounts() {
//        Optional.ofNullable(rayJob.getSkyToK8s()).orElse(Boolean.FALSE)
//    }
}
