package net.statemesh.service.k8s.status;

import io.kubernetes.client.openapi.ApiException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.WorkloadType;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ResourceStatus;
import net.statemesh.service.*;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.DatabaseDTO;
import net.statemesh.service.k8s.ResourceContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static net.statemesh.config.Constants.DEFAULT_FUTURE_TIMEOUT;

@Service
@Slf4j
public class DatabaseStatusService extends ResourceContext {
    private final Map<String, Future<?>> statuses = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AsyncTaskExecutor smTaskExecutor;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final DatabaseService databaseService;

    public DatabaseStatusService(ApplicationService applicationService,
                                 DatabaseService databaseService,
                                 TaskRunService taskRunService,
                                 RayJobService rayJobService,
                                 ContainerService containerService,
                                 KubernetesController kubernetesController,
                                 ApplicationProperties applicationProperties,
                                 AsyncTaskExecutor smTaskExecutor,
                                 @Qualifier("statusScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(applicationService, containerService, taskRunService, rayJobService,
            kubernetesController, applicationProperties);
        this.databaseService = databaseService;
        this.smTaskExecutor = smTaskExecutor;
        this.taskScheduler = taskScheduler;
    }

    protected Context buildDatabaseContext(String databaseId) {
        DatabaseDTO database = databaseService.findOne(databaseId)
            .orElseThrow(() -> new EntityNotFoundException("Database " + databaseId + " not found"));

        // ADD NULL CHECKS
        if (database.getProject() == null) {
            throw new IllegalStateException("Database " + databaseId + " has no project assigned");
        }

        if (database.getProject().getCluster() == null) {
            throw new IllegalStateException("Database " + databaseId + "'s project has no cluster assigned");
        }

        // ADD NAMESPACE CHECK
        if (StringUtils.isEmpty(database.getDeployedNamespace())) {
            throw new IllegalStateException("Database " + databaseId + " has not been deployed yet (no namespace)");
        }

        ApplicationDTO syntheticApp = ApplicationDTO.builder()
            .id(database.getId())
            .name(database.getName())
            .internalName(database.getInternalName())
            .deployedNamespace(database.getDeployedNamespace())
            .project(database.getProject())
            .replicas(database.getReplicas())
            .build();

        syntheticApp.setWorkloadType(WorkloadType.STATEFUL_SET);

        return Context.builder()
            .application(syntheticApp)
            .database(database)
            .podName(null)
            .container(null)
            .build();
    }

    public void startDatabaseStatus(String databaseId, String login) {
        stopStatus(databaseId);

        // ADD TRY-CATCH FOR CONTEXT BUILDING
        Context context;
        try {
            context = buildDatabaseContext(databaseId);
        } catch (Exception e) {
            log.error("Failed to build context for database {}: {}", databaseId, e.getMessage());
            sendStatusUpdate(databaseId, Map.of(
                "type", "disconnect",
                "error", "Database configuration error: " + e.getMessage()
            ));
            return;
        }

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() ->
            smTaskExecutor.submit(() -> {
                TaskResult<List<ResourceStatus>> result;
                try {
                    // ADD NULL CHECK BEFORE CALLING readAppStatus
                    if (context.application().getProject() == null ||
                        context.application().getProject().getCluster() == null) {
                        log.error("Database {} has no cluster configured", databaseId);
                        sendStatusUpdate(databaseId, Map.of(
                            "type", "disconnect",
                            "error", "No cluster configured for this database"
                        ));
                        return;
                    }

                    result = this.kubernetesController.readAppStatus(
                        context.application().getDeployedNamespace(),
                        context.application().getProject().getCluster(),
                        context.application(),
                        null,
                        null
                    ).get(DEFAULT_FUTURE_TIMEOUT, TimeUnit.SECONDS);

                    if (!result.isSuccess() && !Thread.currentThread().isInterrupted()) {
                        log.error("Failed to read status in Kubernetes for database {}", databaseId);
                        sendStatusUpdate(databaseId, Map.of("type", "disconnect"));
                    }

                    readStatus(context.database(), result);
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof ApiException)) {
                        log.error("Error reading database status for {}", databaseId, e);
                        sendStatusUpdate(databaseId, Map.of(
                            "type", "disconnect",
                            "error", "Status read error: " + e.getMessage()
                        ));
                    } else {
                        log.trace("Error reading db status with message {}", e.getMessage());
                    }
                } catch (InterruptedException | TimeoutException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        sendStatusUpdate(databaseId, Map.of("type", "timeout"));
                    }
                }
            }), Duration.ofSeconds(applicationProperties.getMetrics().getStatusPollInterval()));

        // Guard thread
        taskScheduler.schedule(() -> {
            if (!future.isCancelled()) {
                log.debug("Canceling status for database {} due to timeout after {} seconds",
                    databaseId, applicationProperties.getMetrics().getStatusWaitTimeout());
                future.cancel(true);
                statuses.remove(databaseId);

                sendStatusUpdate(databaseId, Map.of(
                    "type", "timeout",
                    "error", "Connection timed out after "
                        + applicationProperties.getMetrics().getStatusWaitTimeout() + " seconds"
                ));
            }
        }, Instant.now().plusSeconds(applicationProperties.getMetrics().getStatusWaitTimeout()));

        this.statuses.put(databaseId, future);
    }

    public SseEmitter registerStatusEmitter(String databaseId) {

        SseEmitter emitter = new SseEmitter(0L); // Infinite timeout
        emitters.put(databaseId, emitter);

        emitter.onCompletion(() -> {
            emitters.remove(databaseId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(databaseId);
        });

        emitter.onError((ex) -> {
            log.error("SSE emitter error for database {}", databaseId, ex);
            emitters.remove(databaseId);
        });

        return emitter;
    }

    private void readStatus(DatabaseDTO database, TaskResult<List<ResourceStatus>> result) {
        if (result != null && result.isSuccess() && result.getValue() != null && !result.getValue().isEmpty()) {
            ResourceStatus status = result.getValue().get(0);

            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("stage", status.getStage().toString());
            statusUpdate.put("message", status.getMessage() != null ? status.getMessage() : "");
            statusUpdate.put("details", status.getDetails() != null ? status.getDetails() : List.of());
            statusUpdate.put("databaseId", database.getId());

            sendStatusUpdate(database.getId(), statusUpdate);
        } else {
            // Log when no status is found
            log.debug("No status found for database {}, result success: {}, has value: {}",
                database.getId(),
                result != null && result.isSuccess(),
                result != null && result.getValue() != null);
        }
    }

    public void stopStatus(String databaseId) {
        Future<?> status = statuses.remove(databaseId);
        if (status != null) {
            log.debug("Stopping status for database {}", databaseId);
            status.cancel(true);
        }

        SseEmitter emitter = emitters.remove(databaseId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Error completing SSE emitter for database {}", databaseId, e);
            }
        }
    }

    private void sendStatusUpdate(String databaseId, Object data) {
        log.trace("Sending status update for database {}", databaseId);
        SseEmitter emitter = emitters.get(databaseId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("status")
                    .data(data));
                log.trace("Successfully sent status update for database {}", databaseId);
            } catch (IOException e) {
                log.error("Failed to send SSE update for database {}", databaseId, e);
                emitters.remove(databaseId);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.warn("Error completing emitter with error", ex);
                }
            }
        }
    }
}
