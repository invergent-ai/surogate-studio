package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.domain.TaskRun;
import net.statemesh.domain.enumeration.*;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.flow.DeleteTaskRunFlow;
import net.statemesh.k8s.flow.TaskRunFlow;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.repository.TaskRunRepository;
import net.statemesh.service.dto.MessageDTO;
import net.statemesh.service.dto.TaskRunDTO;
import net.statemesh.service.dto.TaskRunParamDTO;
import net.statemesh.service.exception.TaskRunException;
import net.statemesh.service.lakefs.LakeFsException;
import net.statemesh.service.lakefs.LakeFsService;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.TaskRunMapper;
import org.apache.commons.collections4.IterableUtils;
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
import static net.statemesh.k8s.util.NamingUtils.resourceName;
import static net.statemesh.service.util.ProjectUtil.updateProject;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskRunService {
    private final ProjectService projectService;
    private final TransactionTemplate transactionTemplate;
    private final TaskRunMapper taskRunMapper;
    private final TaskRunRepository taskRunRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskRunFlow taskRunFlow;
    private final DeleteTaskRunFlow deleteTaskRunFlow;
    private final NotificationService notificationService;
    private final LakeFsService lakeFsService;
    private final KubernetesController kubernetesController;
    private final UserApiKeyService userApiKeyService;

    public TaskRunDTO save(TaskRunDTO taskRunDTO, String login) {
        if (StringUtils.isEmpty(taskRunDTO.getInternalName())) {
            // It is of utmost importance that this happens only once on app creation
            taskRunDTO.setInternalName(resourceName(taskRunDTO.getName()));
        }

        var taskRun = transactionTemplate.execute(tx -> taskRunMapper.toDto(
            taskRunRepository.save(updateRelationships(taskRunDTO)),
            new CycleAvoidingMappingContext()
        ));

        if (!StringUtils.isEmpty(login)) {
            notifyUser(taskRun, login,
                StringUtils.isEmpty(taskRunDTO.getId()) ?
                    MessageDTO.MessageType.CREATE :
                    MessageDTO.MessageType.UPDATE
            );
        }

        return taskRun;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TaskRunDTO submit(TaskRunDTO taskRunDTO, String login) {
        ensurePrerequisites(taskRunDTO, login);
        resolveSavedApiKeys(taskRunDTO, login);

        try {
            taskRunDTO = save(taskRunDTO, login);

            taskRunDTO.setProvisioningStatus(TaskRunProvisioningStatus.DEPLOYING);
            updateProvisioningStatus(taskRunDTO.getId(), TaskRunProvisioningStatus.DEPLOYING);

            var result = taskRunFlow.execute(taskRunDTO);
            taskRunDTO.setDeployedNamespace(taskRunDTO.getProject().getNamespace());
            taskRunDTO.getProject().setCluster(result.getCluster());

            notificationService.notifyUser(taskRunDTO, login,
                result.isSuccess() ? ProcessEvent.DEPLOYED : ProcessEvent.PENDING, null, false);
        } catch (Exception e) {
            log.error("Failed to deploy task", e);
            try {
                cleanupOnError(taskRunDTO);
            } catch (Throwable ignored) {
            }
            updateProvisioningStatus(taskRunDTO.getId(), TaskRunProvisioningStatus.ERROR);
            updateProject(projectService, taskRunDTO);
        }

        return save(taskRunDTO, login);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TaskRunDTO redeploy(TaskRunDTO taskRun, String login) {
        taskRun.setProvisioningStatus(TaskRunProvisioningStatus.COMPLETED);
        updateProvisioningStatus(taskRun.getId(), TaskRunProvisioningStatus.COMPLETED);

        deleteTaskRunFlow.execute(taskRun);

        taskRun.setProvisioningStatus(TaskRunProvisioningStatus.CREATED);
        updateProvisioningStatus(taskRun.getId(), TaskRunProvisioningStatus.CREATED);

        return submit(taskRun, login);
    }

    private void resolveSavedApiKeys(TaskRunDTO taskRun, String login) {
        if (login == null) return;

        if ("true".equals(getParamValue(taskRun, "USE_SAVED_API_KEY"))) {
            String providerStr = getParamValue(taskRun, "SAVED_API_KEY_PROVIDER");
            ApiKeyProvider provider = ApiKeyProvider.fromValue(providerStr);
            userApiKeyService.getDecryptedApiKey(login, provider, ApiKeyType.LLM)
                .ifPresentOrElse(
                    apiKey -> {
                        removeParam(taskRun, "USE_SAVED_API_KEY");
                        removeParam(taskRun, "SAVED_API_KEY_PROVIDER");
                        addOrUpdateParam(taskRun, "DEPLOYED_MODEL_API", apiKey);
                    },
                    () -> log.warn("No saved API key found for provider: {}", provider)
                );
        }

        if ("true".equals(getParamValue(taskRun, "USE_SAVED_JUDGE_API_KEY"))) {
            String providerStr = getParamValue(taskRun, "SAVED_JUDGE_API_KEY_PROVIDER");
            ApiKeyProvider provider = ApiKeyProvider.fromValue(providerStr);
            userApiKeyService.getDecryptedApiKey(login, provider, ApiKeyType.LLM)
                .ifPresent(apiKey -> {
                    removeParam(taskRun, "USE_SAVED_JUDGE_API_KEY");
                    removeParam(taskRun, "SAVED_JUDGE_API_KEY_PROVIDER");
                    addOrUpdateParam(taskRun, "JUDGE_MODEL_API", apiKey);
                });
        }

        if ("true".equals(getParamValue(taskRun, "USE_SAVED_SIMULATOR_API_KEY"))) {
            String providerStr = getParamValue(taskRun, "SAVED_SIMULATOR_API_KEY_PROVIDER");
            ApiKeyProvider provider = ApiKeyProvider.fromValue(providerStr);
            userApiKeyService.getDecryptedApiKey(login, provider, ApiKeyType.LLM)
                .ifPresent(apiKey -> {
                    removeParam(taskRun, "USE_SAVED_SIMULATOR_API_KEY");
                    removeParam(taskRun, "SAVED_SIMULATOR_API_KEY_PROVIDER");
                    addOrUpdateParam(taskRun, "SIMULATOR_MODEL_API", apiKey);
                });
        }
    }

    private String getParamValue(TaskRunDTO taskRun, String key) {
        return taskRun.getParams().stream()
            .filter(p -> key.equals(p.getKey()))
            .map(TaskRunParamDTO::getValue)
            .findFirst()
            .orElse(null);
    }

    private void removeParam(TaskRunDTO taskRun, String key) {
        taskRun.getParams().removeIf(p -> key.equals(p.getKey()));
    }

    private void addOrUpdateParam(TaskRunDTO taskRun, String key, String value) {
        removeParam(taskRun, key);
        taskRun.getParams().add(TaskRunParamDTO.builder().key(key).value(value).build());
    }

    private void cleanupOnError(TaskRunDTO taskRunDTO) {
        if (TaskRunType.IMPORT_HF_MODEL.equals(taskRunDTO.getType()) || TaskRunType.IMPORT_HF_DATASET.equals(taskRunDTO.getType())) {
            // delete lakefs repository

            taskRunDTO.getParams().stream()
                .filter(p -> "lakefs-repo".equals(p.getKey()))
                .findFirst()
                .ifPresent(param -> lakeFsService.deleteRepository(param.getValue()));
        }
    }

    private void ensurePrerequisites(TaskRunDTO taskRunDTO, String login) {
        if (TaskRunType.IMPORT_HF_MODEL.equals(taskRunDTO.getType()) ||
            TaskRunType.IMPORT_HF_DATASET.equals(taskRunDTO.getType())) {
            var hfRepo = IterableUtils.find(taskRunDTO.getParams(), p -> "hf-repo-id".equals(p.getKey()));
            if (hfRepo == null || StringUtils.isEmpty(hfRepo.getValue())) {
                throw new TaskRunException("HF Repository not set");
            }
            var lakeFsRepo = NamingUtils.lakeFsRepoFromHfRepo(hfRepo.getValue());
            String repoId = generateRepoId(login, lakeFsRepo);
            var existingRepo = lakeFsService.getRepository(repoId);
            if (existingRepo != null) {
                throw new TaskRunException(String.format("A Data Hub repository with name '%s' already exists", lakeFsRepo));
            } else {
                if (TaskRunType.IMPORT_HF_MODEL.equals(taskRunDTO.getType())) {
                    lakeFsService.createModelRepository(lakeFsRepo, hfRepo.getValue());
                } else {
                    lakeFsService.createDatasetRepository(lakeFsRepo, hfRepo.getValue());
                }
            }
            taskRunDTO.getParams().add(TaskRunParamDTO.builder().key("hf-token").value("").build());
            taskRunDTO.getParams().add(TaskRunParamDTO.builder().key("lakefs-repo").value(repoId).build());
            taskRunDTO.getParams().add(TaskRunParamDTO.builder().key("lakefs-branch").value("main").build());
        }
    }

    private String generateRepoId(String email, String repoName) {
        final int MAX_ID_LENGTH = 63;
        String sanitizedUser = email
            .toLowerCase()
            .replaceAll("[^a-z0-9-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        int maxRepoLength = MAX_ID_LENGTH - sanitizedUser.length() - 1;
        String sanitizedRepo = repoName
            .toLowerCase()
            .replaceAll("[^a-z0-9-]", "-");
        if (sanitizedRepo.length() > maxRepoLength) {
            throw new LakeFsException(
                String.format("Repository name too long. Maximum allowed length is %d characters.",
                    maxRepoLength)
            );
        }
        return sanitizedUser + "-" + sanitizedRepo;
    }

    private TaskRun updateRelationships(TaskRunDTO taskRunDTO) {
        updateProject(projectService, taskRunDTO);
        final TaskRun taskRun = taskRunMapper.toEntity(taskRunDTO, new CycleAvoidingMappingContext());
        taskRun.getParams().forEach(param -> param.setTaskRun(taskRun));
        return taskRun;
    }

    public void notifyUser(@Nullable TaskRunDTO taskRun, String login, MessageDTO.MessageType type) {
        log.debug("Notifying user {} of task run {} update type {}", login,
            Objects.requireNonNull(taskRun).getName(), type);
        messagingTemplate.convertAndSend("/topic/message/" + login,
            MessageDTO.builder()
                .type(type)
                .tasks(Collections.singletonList(taskRun))
                .build()
        );
    }

    public void updateProvisioningStatus(String id, TaskRunProvisioningStatus status) {
        transactionTemplate.executeWithoutResult(tx -> taskRunRepository.updateTaskRunProvisioningStatus(id, status));
    }

    public void updateCompletedStatus(String id, String status) {
        transactionTemplate.executeWithoutResult(tx -> taskRunRepository.updateTaskRunCompletedStatus(id, status));
    }

    public void updateEndTime(String id, Instant endTime) {
        transactionTemplate.executeWithoutResult(tx -> taskRunRepository.updateEndTime(id, endTime));
    }

    public void updateStartTime(String id, Instant startTime) {
        transactionTemplate.executeWithoutResult(tx -> taskRunRepository.updateStartTime(id, startTime));
    }

    public void updatePodAndContainer(String id, String podName, String container) {
        transactionTemplate.executeWithoutResult(tx -> taskRunRepository.updatePodAndContainer(id, podName, container));
    }

    @Transactional(readOnly = true)
    public Set<TaskRunDTO> findTaskRunsById(String[] ids) {
        return taskRunRepository.findAllById(Arrays.asList(ids))
            .stream().map(t -> taskRunMapper.toDto(t, new CycleAvoidingMappingContext()))
            .collect(Collectors.toSet());
    }

    @Transactional
    public void cancel(String taskId) {
        var optTask = taskRunRepository.findById(taskId)
            .map(t -> taskRunMapper.toDto(t, new CycleAvoidingMappingContext()));

        if (optTask.isEmpty())
            return;

        var task = optTask.get();
        try {
            kubernetesController.cancelTaskRun(
                Objects.isNull(task.getDeployedNamespace()) ? task.getProject().getNamespace() : task.getDeployedNamespace(),
                task.getProject().getCluster(),
                task
            ).get(DELETE_NODE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            task.setProvisioningStatus(TaskRunProvisioningStatus.CANCELLED);
            task.setEndTime(Instant.now());
            save(task, null);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Could not cancel task run {}", task, e);
            throw new RuntimeException("Could not cancel task run " + task.getName(), e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void delete(String taskId, String login) {
        var taskRunDTO = transactionTemplate.execute(tx -> {
            Optional<TaskRun> optTask = taskRunRepository.findById(taskId);
            return optTask.map(taskRun -> taskRunMapper.toDto(taskRun, new CycleAvoidingMappingContext())).orElse(null);
        });
        if (taskRunDTO == null) {
            return;
        }

        deleteTaskRunFlow.execute(taskRunDTO);
        transactionTemplate.executeWithoutResult(tx -> taskRunRepository.deleteById(taskId));

        notifyUser(taskRunDTO, login, MessageDTO.MessageType.DELETE);
        notificationService.notifyUser(taskRunDTO, login, ProcessEvent.DELETED, null, false);
    }

    @Transactional(readOnly = true)
    public Optional<TaskRunDTO> findOne(String id) {
        return taskRunRepository.findOneWithEagerRelationships(id)
            .map(o -> taskRunMapper.toDto(o, new CycleAvoidingMappingContext()));
    }
}
