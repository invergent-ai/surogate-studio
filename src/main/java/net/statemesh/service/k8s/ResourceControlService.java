package net.statemesh.service.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.ApplicationMode;
import net.statemesh.domain.enumeration.DatabaseStatus;
import net.statemesh.domain.enumeration.WorkloadType;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.flow.CreateModelFlow;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.task.control.ControlTask;
import net.statemesh.k8s.task.control.ControlTask.ControlObject;
import net.statemesh.service.*;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.DatabaseDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.statemesh.config.Constants.*;

@Service
@Slf4j
public class ResourceControlService extends ResourceContext {
    private final AsyncTaskExecutor smTaskExecutor;
    private final ObjectMapper objectMapper;
    private final DatabaseService databaseService;

    public ResourceControlService(ApplicationService applicationService,
                                  TaskRunService taskRunService,
                                  RayJobService rayJobService,
                                  DatabaseService databaseService,
                                  ContainerService containerService,
                                  KubernetesController kubernetesController,
                                  ApplicationProperties applicationProperties,
                                  AsyncTaskExecutor smTaskExecutor,
                                  ObjectMapper objectMapper) {
        super(applicationService, containerService, taskRunService, rayJobService,
            kubernetesController, applicationProperties);
        this.smTaskExecutor = smTaskExecutor;
        this.objectMapper = objectMapper;
        this.databaseService = databaseService;
    }

    protected Context buildDatabaseContext(String databaseId) {
        DatabaseDTO database = databaseService.findOne(databaseId)
            .orElseThrow(() -> new EntityNotFoundException("Database " + databaseId + " not found"));

        // Create a completely new ApplicationDTO to avoid any inherited values
        ApplicationDTO syntheticApp = new ApplicationDTO();
        syntheticApp.setId(database.getId());
        syntheticApp.setName(database.getName());
        syntheticApp.setInternalName(database.getInternalName());
        syntheticApp.setDeployedNamespace(database.getDeployedNamespace());
        syntheticApp.setProject(database.getProject());
        syntheticApp.setReplicas(database.getReplicas());
        syntheticApp.setWorkloadType(WorkloadType.STATEFUL_SET);  // Explicitly set for databases

        log.debug("Built database context with workload type: {} for database: {}",
            syntheticApp.getWorkloadType(), database.getName());

        return Context.builder()
            .application(syntheticApp)
            .database(database)
            .podName(null)
            .container(null)
            .build();
    }

    public boolean startApplication(String applicationId, String component) {
        log.info("Starting application {}", applicationId);
        final TaskResult<Void> result = controlApp(applicationId, ControlTask.ControlCommand.START, component, null);
        log.info("Application {} started: {}", applicationId, result.isSuccess() || result.isWaitTimeout());
        return result.isSuccess() || result.isWaitTimeout();
    }

    public boolean stopApplication(String applicationId, String component) {
        log.info("Stopping application {}", applicationId);
        final TaskResult<Void> result = controlApp(applicationId, ControlTask.ControlCommand.STOP, component, null);
        log.info("Application {} stopped: {}", applicationId, result.isSuccess() || result.isWaitTimeout());
        return result.isSuccess() || result.isWaitTimeout();
    }

    public boolean restartApplication(String applicationId, String component) {
        log.info("Restarting application {}", applicationId);
        final TaskResult<Void> result = controlApp(applicationId, ControlTask.ControlCommand.RESTART_ROLLOUT, component, null);
        log.info("Application {} restarted: {}", applicationId, result.isSuccess() || result.isWaitTimeout());
        return result.isSuccess() || result.isWaitTimeout();
    }

    public boolean scaleApplication(String applicationId, Integer replicas) {
        log.info("Scaling application {} to {} replicas", applicationId, replicas);
        final TaskResult<Void> result = controlApp(applicationId, ControlTask.ControlCommand.SCALE, null, replicas);
        log.info("Application {} scaled: {}", applicationId, result.isSuccess() || result.isWaitTimeout());
        return result.isSuccess() || result.isWaitTimeout();
    }

    public boolean startDatabase(String databaseId) {
        log.info("Starting database {}", databaseId);
        final TaskResult<Void> result = controlDatabase(databaseId, ControlTask.ControlCommand.START);
        log.info("Database {} started: {}", databaseId, result.isSuccess() || result.isWaitTimeout());
        return result.isSuccess() || result.isWaitTimeout();
    }

    public boolean stopDatabase(String databaseId) {
        log.info("Stopping database {}", databaseId);
        final TaskResult<Void> result = controlDatabase(databaseId, ControlTask.ControlCommand.STOP);
        boolean success = result.isSuccess() || result.isWaitTimeout();
        log.info("Database {} stopped: {}", databaseId, success);
        return success;
    }


    public void stopUserApplications(String login) {
        applicationService.findUserApplications(login).stream()
            .filter(app -> !StringUtils.isEmpty(app.getDeployedNamespace()))
            .forEach(
                application -> smTaskExecutor.execute(
                    () -> stopApplication(application.getId(), null)));
    }

    public void stopUserDatabases(String login) {
        databaseService.findUserDatabases(login).stream()
            .filter(db -> !StringUtils.isEmpty(db.getDeployedNamespace()))
            .forEach(
                db -> smTaskExecutor.execute(
                    () -> stopDatabase(db.getId())));
    }

    private TaskResult<Void> controlApp(String appId, ControlTask.ControlCommand command, String component, Integer replicas) {
        var context = buildAppContext(appId, null, null);
        if (context.application().getMode().equals(ApplicationMode.MODEL)) {
            context = switch (component) {
                case MODEL_COMPONENT_ROUTER ->
                    buildAppContext(CreateModelFlow.scaffoldRouterApplication(context.application(), objectMapper), null, null);
                case MODEL_COMPONENT_WORKER ->
                    buildAppContext(CreateModelFlow.scaffoldWorkerApplication(context.application(), objectMapper), null, null);
                case MODEL_COMPONENT_CACHE ->
                    buildAppContext(CreateModelFlow.scaffoldCacheApplication(context.application(), objectMapper), null, null);
                default -> throw new IllegalStateException("Unexpected model component: " + component);
            };
        }
        try {
            return this.kubernetesController.control(
                context.application().getDeployedNamespace(),
                context.application().getProject().getCluster(),
                ControlObject.APPLICATION,
                command,
                context.application(),
                null,
                replicas
            ).get(DEFAULT_FUTURE_TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private TaskResult<Void> controlDatabase(String databaseId, ControlTask.ControlCommand command) {
        Context context = buildDatabaseContext(databaseId);
        try {
            return this.kubernetesController.control(
                context.database().getDeployedNamespace(),
                context.database().getProject().getCluster(),
                ControlObject.APPLICATION,
                command,
                context.application(),
                null,
                null
            ).get(DEFAULT_FUTURE_TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // If deployment not found, check if it's actually a PostgreSQL cluster
            if (e.getCause() != null && e.getCause().getMessage().contains("404")) {
                log.warn("Database deployment not found for {}, it might already be stopped or use a different resource type", databaseId);
                return TaskResult.success();
            }
            throw new RuntimeException(e);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
