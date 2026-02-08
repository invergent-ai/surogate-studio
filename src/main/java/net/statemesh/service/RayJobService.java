package net.statemesh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.RayJob;
import net.statemesh.domain.enumeration.ProcessEvent;
import net.statemesh.domain.enumeration.RayJobProvisioningStatus;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.flow.CreateRayJobFlow;
import net.statemesh.k8s.flow.DeleteRayJobFlow;
import net.statemesh.k8s.flow.DeleteTaskRunFlow;
import net.statemesh.k8s.flow.TaskRunFlow;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.repository.RayJobRepository;
import net.statemesh.service.dto.*;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.RayJobMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static net.statemesh.config.K8Timeouts.DELETE_NODE_TIMEOUT_SECONDS;
import static net.statemesh.k8s.util.ApiUtils.executeInsidePod;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.*;
import static net.statemesh.service.util.MixinUtil.*;
import static net.statemesh.service.util.ProjectUtil.updateProject;
import static net.statemesh.service.util.SkyUtil.rayJobToSkyTaskRun;
import static net.statemesh.service.util.SkyUtil.setupSkyConfig;

@Service
@RequiredArgsConstructor
@Slf4j
public class RayJobService {
    private final CreateRayJobFlow createRayJobFlow;
    private final DeleteRayJobFlow deleteRayJobFlow;
    private final TaskRunFlow taskRunFlow;
    private final DeleteTaskRunFlow deleteTaskRunFlow;
    private final RayJobRepository rayJobRepository;
    private final RayJobMapper rayJobMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;
    private final KubernetesController kubernetesController;
    private final ObjectMapper objectMapper;
    private final ObjectMapper yamlMapper = new ObjectMapper(
        YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build()
    );
    private final ApplicationProperties applicationProperties;

    public RayJobDTO save(RayJobDTO rayJobDTO, String login) {
        log.debug("Request to save RayJob : {}", rayJobDTO);

        var rayJob = transactionTemplate.execute(tx -> rayJobMapper.toDto(
            rayJobRepository.save(updateRelationships(rayJobDTO)),
            new CycleAvoidingMappingContext()
        ));

        notifyUser(rayJob, login,
            StringUtils.isEmpty(rayJobDTO.getId()) ?
                MessageDTO.MessageType.CREATE :
                MessageDTO.MessageType.UPDATE
        );

        return rayJob;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RayJobDTO deploy(RayJobDTO rayJobDTO, String login) {
        if (Optional.ofNullable(applicationProperties.getDisableLocalInfraTraining()).orElse(Boolean.FALSE) &&
            (!Optional.ofNullable(rayJobDTO.getRunInTheSky()).orElse(Boolean.FALSE) ||
            Optional.ofNullable(rayJobDTO.getSkyToK8s()).orElse(Boolean.FALSE))) {
            throw new RuntimeException("Training on local infrastructure is disabled");
        }

        try {
            rayJobDTO.setProvisioningStatus(RayJobProvisioningStatus.DEPLOYING);
            updateProvisioningStatus(rayJobDTO.getId(), RayJobProvisioningStatus.DEPLOYING);

            TaskResult<String> result =
                Optional.ofNullable(rayJobDTO.getRunInTheSky()).orElse(Boolean.FALSE) ?
                    runInTheSky(rayJobDTO) :
                    createRayJobFlow.execute(rayJobDTO);
            rayJobDTO.setDeployedNamespace(
                EXTERNAL_RAY_CLUSTER ? EXTERNAL_RAY_CLUSTER_DEFAULT_NAMESPACE : rayJobDTO.getProject().getNamespace()
            );
            rayJobDTO.getProject().setCluster(result.getCluster());
            rayJobDTO.setChatHostName(result.getValue());

            notificationService.notifyUser(rayJobDTO, login,
                result.isSuccess() ? ProcessEvent.DEPLOYED : ProcessEvent.PENDING, null, false);
        } catch (K8SException e) {
            log.error("Failed to deploy ray job", e);
            rayJobDTO.setProvisioningStatus(RayJobProvisioningStatus.ERROR);
            updateProject(projectService, rayJobDTO);
        }

        return save(rayJobDTO, login);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RayJobDTO redeploy(RayJobDTO rayJob, String login) {
        rayJob.setProvisioningStatus(RayJobProvisioningStatus.COMPLETED);
        updateProvisioningStatus(rayJob.getId(), RayJobProvisioningStatus.COMPLETED);

        if (Optional.ofNullable(rayJob.getRunInTheSky()).orElse(Boolean.FALSE)) {
            deleteFromTheSky(rayJob);
        } else {
            deleteRayJobFlow.execute(rayJob);
        }

        rayJob.setProvisioningStatus(RayJobProvisioningStatus.CREATED);
        updateProvisioningStatus(rayJob.getId(), RayJobProvisioningStatus.CREATED);

        return deploy(rayJob, login);
    }

    /**
     * Run on SkyPilot using Tekton backend
     *
     * @param rayJob - RayJobDTO
     * @return TaskResult<String>
     */
    private TaskResult<String> runInTheSky(RayJobDTO rayJob) {
         return taskRunFlow.execute(
             rayJobToSkyTaskRun(rayJob, kubernetesController.getApplicationProperties())
         );
    }

    private void deleteFromTheSky(RayJobDTO rayJob) {
        stopSkyCluster(rayJob);
        deleteTaskRunFlow.execute(
            rayJobToSkyTaskRun(rayJob, kubernetesController.getApplicationProperties())
        );
    }

    private void cancelFromTheSky(RayJobDTO rayJob) throws ExecutionException, InterruptedException, TimeoutException {
        stopSkyCluster(rayJob);
        kubernetesController.cancelTaskRun(
            Objects.isNull(rayJob.getDeployedNamespace()) ?
                rayJob.getProject().getNamespace() :
                rayJob.getDeployedNamespace(),
            rayJob.getProject().getCluster(),
            rayJobToSkyTaskRun(rayJob, kubernetesController.getApplicationProperties())
        ).get(DELETE_NODE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void stopSkyCluster(RayJobDTO rayJob) {
        if (StringUtils.isEmpty(rayJob.getDeployedNamespace())) {
            return;
        }
        executeInsidePod(
            kubernetesController.getApi(rayJob.getProject().getCluster()),
            rayJob.getDeployedNamespace(),
            rayJob.getInternalName() + "-pod",
            "step-" + TEKTON_JOB_NAME,
            new String[]{"down-sky"}
        );
    }

    public void loadInternalsAndDumpTrainingConfig(RayJobDTO rayJobDTO) {
        if (rayJobDTO.getRayClusterShapePojo() == null) {
            rayJobDTO.setRayClusterShapePojo(defaultRayClusterShape());
        }
        try {
            rayJobDTO.setRayClusterShape(
                objectMapper.writeValueAsString(
                    rayJobDTO.getRayClusterShapePojo()
                )
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (rayJobDTO.getTrainingConfigPojo() == null) {
            return;
        }

        try {
            addSerializationMixins(yamlMapper, rayJobDTO);
            rayJobDTO.setTrainingConfig(
                yamlMapper.writeValueAsString(
                    Boolean.TRUE.equals(rayJobDTO.getUseAxolotl()) ?
                        loadAxolotlTrainingConfigInternals(
                            rayJobDTO.getTrainingConfigPojo(),
                            rayJobDTO.getRayClusterShapePojo(),
                            rayJobDTO.getInternalName()
                        ) :
                        loadTrainingConfigInternals(
                            rayJobDTO.getTrainingConfigPojo(),
                            rayJobDTO.getRayClusterShapePojo(),
                            rayJobDTO.getInternalName()
                        )
                )
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        dumpSkyConfig(rayJobDTO);
    }

    public void loadTrainingConfig(Optional<RayJobDTO> rayJobDTO) {
        if (rayJobDTO.isEmpty()) {
            return;
        }
        if (!StringUtils.isEmpty(rayJobDTO.get().getTrainingConfig())) {
            try {
                addDeserializationMixins(yamlMapper, rayJobDTO.get());
                rayJobDTO.get().setTrainingConfigPojo(
                    yamlMapper.readValue(rayJobDTO.get().getTrainingConfig(), TrainingConfigDTO.class)
                );
                if (Boolean.TRUE.equals(rayJobDTO.get().getUseAxolotl())) {
                    addValuesDeserializationMixin(rayJobDTO.get().getTrainingConfigPojo());
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        if (!StringUtils.isEmpty(rayJobDTO.get().getRayClusterShape())) {
            try {
                rayJobDTO.get().setRayClusterShapePojo(
                    objectMapper.readValue(rayJobDTO.get().getRayClusterShape(), RayClusterShape.class)
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        if (!StringUtils.isEmpty(rayJobDTO.get().getSkyConfig())) {
            try {
                addSkyMixins(yamlMapper);
                rayJobDTO.get().setSkyConfigPojo(
                    yamlMapper.readValue(rayJobDTO.get().getSkyConfig(), SkyConfigDTO.class)
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private TrainingConfigDTO loadAxolotlTrainingConfigInternals(TrainingConfigDTO trainingConfig,
                                                                 RayClusterShape rayClusterShape,
                                                                 String jobId) {
        return loadBaseTrainingConfigInternals(trainingConfig, Boolean.TRUE)
            .withPlugins(
                List.of(
                    "axolotl.integrations.aim.AimPlugin"
                )
            )
            .withUseRay(Boolean.TRUE)
            .withRayNumWorkers(rayClusterShape.getNumNodes() * rayClusterShape.getGpusPerWorker())
            .withAdapter(isQlora(trainingConfig) ? "qlora" : isLora(trainingConfig) ? "lora" : null)
            .withLoadIn4bit(is4bit(trainingConfig))
            .withGradientCheckpointingKwargs(TrainingConfigDTO.GradientCheckpointingKwargs.builder()
                .useReentrant(Boolean.FALSE)
                .build())
            .withAim(TrainingConfigDTO.Aim.builder()
                .aimEnable(Boolean.TRUE)
                .aimRepo(AIM_DIR)
                .aimExperiment(jobId)
                .build());
    }

    private TrainingConfigDTO loadTrainingConfigInternals(TrainingConfigDTO trainingConfig,
                                                          RayClusterShape rayClusterShape,
                                                          String jobId) {
        return loadBaseTrainingConfigInternals(trainingConfig, Boolean.FALSE)
            .withDistributed(TrainingConfigDTO.Distributed.builder()
                .rayAddress("auto")
                .numNodes(rayClusterShape.getNumNodes())
                .gpusPerNode(rayClusterShape.getGpusPerWorker())
                .build())
            .withLossScale(Optional.ofNullable(trainingConfig.getTrainOnInputs()).orElse(Boolean.FALSE) ? "all" : null)
            .withReportTo("aim")
            .withAimRepo(AIM_DIR)
            .withAimExperiment(jobId);
    }

    private TrainingConfigDTO loadBaseTrainingConfigInternals(TrainingConfigDTO trainingConfig, Boolean useAxolotl) {
        return trainingConfig
            .withBaseModel(RAY_WORK_DIR + "/model")
            .withOutputDir(RAY_WORK_DIR + "/outputs" +
                (useAxolotl &&
                    Optional.ofNullable(trainingConfig.getLora()).orElse(Boolean.FALSE) ? "/lora" : ""))
            .withDatasetsPath(RAY_WORK_DIR);
    }

    public void dumpSkyConfig(RayJobDTO rayJobDTO) {
        if (!Optional.ofNullable(rayJobDTO.getRunInTheSky()).orElse(Boolean.FALSE) ||
            rayJobDTO.getSkyConfigPojo() == null) {
            return;
        }

        try {
            addSkyMixins(yamlMapper);
            rayJobDTO.setSkyConfig(
                yamlMapper.writeValueAsString(
                    loadSkyConfigInternals(rayJobDTO)
                )
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private SkyConfigDTO loadSkyConfigInternals(RayJobDTO rayJob) {
        return setupSkyConfig(rayJob.getSkyConfigPojo(), rayJob)
            .withName(rayJob.getInternalName())
            .withNumNodes(rayJob.getRayClusterShapePojo().getNumNodes())
            .withWorkDir(".")
            .withFileMounts(Map.of(
                RAY_WORK_DIR, RAY_WORK_DIR
            ));
    }

    public void initNames(RayJobDTO rayJobDTO) {
        if (StringUtils.isEmpty(rayJobDTO.getName())) {
            // Job names are usually an internal matter (not for UI)
            rayJobDTO.setName(jobName(rayJobDTO.getType()));
        }
        if (StringUtils.isEmpty(rayJobDTO.getInternalName())) {
            // It is of utmost importance that this happens only once on job creation
            rayJobDTO.setInternalName(resourceName(rayJobDTO.getName()));
        }
        if (StringUtils.isEmpty(rayJobDTO.getWorkDirVolumeName())) {
            // It is of utmost importance that this happens only once on job creation
            rayJobDTO.setWorkDirVolumeName(workDirVolumeName(RAY_WORKDIR_VOLUME_NAME));
        }
    }

    private RayJob updateRelationships(RayJobDTO rayJobDTO) {
        updateProject(projectService, rayJobDTO);
        final RayJob rayJob = rayJobMapper.toEntity(rayJobDTO, new CycleAvoidingMappingContext());
        rayJob.getEnvVars().forEach(envVar -> envVar.setJob(rayJob));
        return rayJob;
    }

    @Transactional(readOnly = true)
    public Optional<RayJobDTO> findOne(String id) {
        return rayJobRepository.findOneWithEagerRelationships(id)
            .map(o -> rayJobMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    public void updateProvisioningStatus(String id, RayJobProvisioningStatus status) {
        transactionTemplate.executeWithoutResult(tx -> rayJobRepository.updateRayJobProvisioningStatus(id, status));
    }

    public void updateCompletedStatus(String id, String status) {
        transactionTemplate.executeWithoutResult(tx -> rayJobRepository.updateRayJobCompletedStatus(id, status));
    }

    public void updateStartTime(String id, Instant startTime) {
        transactionTemplate.executeWithoutResult(tx -> rayJobRepository.updateStartTime(id, startTime));
    }

    public void updateEndTime(String id, Instant endTime) {
        transactionTemplate.executeWithoutResult(tx -> rayJobRepository.updateEndTime(id, endTime));
    }

    public void updateSubmissionId(String id, String submissionId) {
        transactionTemplate.executeWithoutResult(tx -> rayJobRepository.updateSubmissionId(id, submissionId));
    }

    public void updatePodAndContainer(String id, String podName, String container) {
        transactionTemplate.executeWithoutResult(tx -> rayJobRepository.updatePodAndContainer(id, podName, container));
    }

    @Transactional(readOnly = true)
    public Set<RayJobDTO> findRayJobsById(String[] ids) {
        return rayJobRepository.findAllById(Arrays.asList(ids))
            .stream().map(t -> rayJobMapper.toDto(t, new CycleAvoidingMappingContext()))
            .collect(Collectors.toSet());
    }

    @Transactional
    public void cancel(String id, String login) {
        var optJob = rayJobRepository.findById(id)
            .map(t -> rayJobMapper.toDto(t, new CycleAvoidingMappingContext()));
        if (optJob.isEmpty()) {
            return;
        }

        var job = optJob.get();
        try {
            if (Optional.ofNullable(job.getRunInTheSky()).orElse(Boolean.FALSE)) {
                cancelFromTheSky(job);
            } else {
                kubernetesController.cancelRayJob(
                    Objects.isNull(job.getDeployedNamespace()) ? job.getProject().getNamespace() : job.getDeployedNamespace(),
                    job.getProject().getCluster(),
                    job
                ).get(DELETE_NODE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }

            job.setProvisioningStatus(RayJobProvisioningStatus.CANCELLED);
            job.setEndTime(Instant.now());
            save(job, login);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Could not cancel ray job {}", job, e);
            throw new RuntimeException("Could not cancel ray job " + job.getName(), e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void delete(String id, String login) {
        log.debug("Request to delete RayJob : {}", id);

        var rayJobDTO = transactionTemplate.execute(tx -> {
            Optional<RayJob> optJob = rayJobRepository.findById(id);
            if (optJob.isEmpty()) {
                return null;
            }
            var rayJob = optJob.get();
            return rayJobMapper.toDto(rayJob, new CycleAvoidingMappingContext());
        });
        if (rayJobDTO == null) {
            return;
        }

        if (Optional.ofNullable(rayJobDTO.getRunInTheSky()).orElse(Boolean.FALSE)) {
            deleteFromTheSky(rayJobDTO);
        } else {
            deleteRayJobFlow.execute(rayJobDTO);
        }
        transactionTemplate.executeWithoutResult(tx -> rayJobRepository.deleteById(id));

        notifyUser(rayJobDTO, login, MessageDTO.MessageType.DELETE);
        notificationService.notifyUser(rayJobDTO, login, ProcessEvent.DELETED, null, false);
    }

    public void restoreTransient(RayJobDTO source, RayJobDTO dest) {
        dest.setRayClusterShapePojo(source.getRayClusterShapePojo());
        dest.setSkyConfigPojo(source.getSkyConfigPojo());
        dest.setTrainingConfigPojo(source.getTrainingConfigPojo());
    }

    public void notifyUser(@Nullable RayJobDTO rayJobDTO, String login, MessageDTO.MessageType type) {
        log.debug("Notifying user {} of ray job {} update type {}", login,
            Objects.requireNonNull(rayJobDTO).getName(), type);
        messagingTemplate.convertAndSend("/topic/message/" + login,
            MessageDTO.builder()
                .type(type)
                .jobs(Collections.singletonList(rayJobDTO))
                .build()
        );
    }

    private boolean isLora(TrainingConfigDTO trainingConfig) {
        return Optional.ofNullable(trainingConfig.getLora()).orElse(Boolean.FALSE);
    }

    private boolean isQlora(TrainingConfigDTO trainingConfig) {
        return Optional.ofNullable(trainingConfig.getQloraFp8()).orElse(Boolean.FALSE) ||
            Optional.ofNullable(trainingConfig.getQloraFp4()).orElse(Boolean.FALSE) ||
            Optional.ofNullable(trainingConfig.getQloraBnb()).orElse(Boolean.FALSE);
    }

    private boolean is4bit(TrainingConfigDTO trainingConfig) {
        return Optional.ofNullable(trainingConfig.getQloraFp4()).orElse(Boolean.FALSE) ||
            Optional.ofNullable(trainingConfig.getQloraBnb()).orElse(Boolean.FALSE);
    }

    private RayClusterShape defaultRayClusterShape() {
        return RayClusterShape.builder()
            .numNodes(1)
            .gpusPerWorker(2)
            .useHeadAsWorker(Boolean.FALSE)
            .headGpus(2)
            .testVllmTp(2)
            .build();
    }
}
