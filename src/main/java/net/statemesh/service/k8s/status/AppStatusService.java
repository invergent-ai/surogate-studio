package net.statemesh.service.k8s.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiException;
import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.ApplicationStatus;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ResourceStatus;
import net.statemesh.service.*;
import net.statemesh.service.dto.ApplicationDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static net.statemesh.config.K8Timeouts.COPY_FILE_TIMEOUT_SECONDS;
import static net.statemesh.config.K8Timeouts.READ_STATUS_TIMEOUT_SECONDS;

@Service("appStatusService")
@Slf4j
public class AppStatusService extends PollingEventStreamService {
    private final MessageSource messageSource;
    protected final ObjectMapper objectMapper;

    public AppStatusService(ApplicationService applicationService,
                            ContainerService containerService,
                            TaskRunService taskRunService,
                            RayJobService rayJobService,
                            KubernetesController kubernetesController,
                            ApplicationProperties applicationProperties,
                            MessageSource messageSource,
                            @Qualifier("statusScheduler") ThreadPoolTaskScheduler taskScheduler,
                            ObjectMapper objectMapper) {
        super(applicationService, containerService, taskRunService, rayJobService, kubernetesController, applicationProperties, taskScheduler);
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public PollHandle start(Long pollInterval, Long pollTimeout, String... emId) {
        Context initialContext = buildAppContext(emId[0], null, null);
        if (ApplicationStatus.BUILDING.equals(initialContext.application().getStatus())) {
            log.trace("Application {} is BUILDING; status polling not started yet", emId[0]);
            return null;
        }
        super.start(pollInterval, pollTimeout, emId);
        return null;
    }

    @Override
    protected void onTick(String... emId) throws InterruptedException, TimeoutException {
        Context ctx;
        try {
            ctx = buildAppContext(emId[0], null, null);
        } catch (EntityNotFoundException ex) {
            completeEmitters(true, "deleted", emId);
            return;
        }

        if (ApplicationStatus.BUILDING.equals(ctx.application().getStatus())) {
            log.trace("Application {} is BUILDING during poll; skipping this cycle", emId[0]);
            return; // skip until out of BUILDING; we chose not to auto-cancel
        }

        try {
            var result = getAppStatus(ctx);
            if (result == null) {
                return; // error already logged
            }

            var statuses = updateAppStatusFromResources(ctx.application(), result.getValue());

            try {
                if (statuses != null) {
                    sendEvent("status", AppStatus.builder()
                            .applicationId(emId[0])
                            .status(ctx.application().getStatus())
                            .resourceStatus(statuses)
                            .build(),
                        emId);
                }
            } catch (Exception e) {
                log.error("Failed to send status update to SSE for application {}", emId, e);
            }
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ApiException)) {
                throw new RuntimeException(e);
            } else {
                log.trace("Error reading app status with message {}", e.getMessage());
            }
        }
    }

    protected TaskResult<List<ResourceStatus>> getAppStatus(Context ctx) throws ExecutionException, InterruptedException, TimeoutException {
        var app = ctx.application();
        TaskResult<List<ResourceStatus>> result;

        if (ctx.application().getProject().getCluster() != null) {
            final var client = deleteDanglingPods(ctx);
            if (client == null) {
                ctx.application().setMessage("There was a technical error on our side. We are working on fixing it.");
                return null;
            }

            result = this.kubernetesController.readAppStatus(
                Objects.isNull(app.getDeployedNamespace()) ? app.getInternalName() : app.getDeployedNamespace(),
                app.getProject().getCluster(),
                app,
                null,
                null
            ).get(COPY_FILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.isSuccess() && !Thread.currentThread().isInterrupted()) {
                log.error("Failed to read status in Kubernetes for application {}", app.getId());
                return null;
            }
        } else {
            // app was never deployed
            result = TaskResult.<List<ResourceStatus>>builder()
                .value(Collections.emptyList())
                .success(true)
                .build();
        }

        return result;
    }

    public Collection<ResourceStatus> getResourceStatus(Context context) throws Exception {
        var app = context.application();
        var result = this.kubernetesController.readAppStatus(
            app.getDeployedNamespace(),
            app.getProject().getCluster(),
            app,
            null,
            null
        ).get(READ_STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (result.isSuccess()) {
            return updateAppStatusFromResources(app, result.getValue());
        } else {
            log.error("Failed to read status in Kubernetes for application {}", app.getId());
            return Collections.emptyList();
        }
    }

    protected List<ResourceStatus> updateAppStatusFromResources(ApplicationDTO application, List<ResourceStatus> resourceStatuses) {
//        if (application.getStatus() == ApplicationStatus.ERROR) {
//            // error has precedence over all statuses
//            return resourceStatuses;
//        }

        if (resourceStatuses == null || resourceStatuses.isEmpty()) {
            if (!ApplicationStatus.DELETING.equals(application.getStatus()) && !ApplicationStatus.DEPLOYING.equals(application.getStatus())) {
                application.setStatus(ApplicationStatus.CREATED);
                applicationService.updateStatus(application.getId(), ApplicationStatus.CREATED);
            }
            return resourceStatuses; // Nothing to enrich
        }

        if (application.getStatus() == ApplicationStatus.DELETING) {
            // No status updates while deleting
            return resourceStatuses;
        }

        // per-pod message enrichment (no application status mutation here)
        for (ResourceStatus rs : resourceStatuses) {
            if (ApplicationStatus.DEPLOYING.equals(application.getStatus())) {
                boolean hasRunningContainers = rs.getContainerStatuses() == null ? Boolean.FALSE :
                    rs.getContainerStatuses().stream()
                        .anyMatch(status -> ResourceStatus.ContainerStatusStage.RUNNING.equals(status.getStage()));

                if (hasRunningContainers) {
                    String pending = pendingContainers(rs.getContainerStatuses());
                    if (StringUtils.isNotBlank(pending)) {
                        rs.message("Waiting for containers to become Ready: " + pending);
                    }
                } else {
                    rs.message("Application is deploying");
                }

                if (rs.getContainerStatuses() != null) {
                    if (rs.getContainerStatuses().size() == 1) {
                        var cs = rs.getContainerStatuses().getFirst();
                        if (ResourceStatus.ContainerStatusStage.TERMINATED.equals(cs.getLastStage())) {
                            String reason = cs.getLastStageTerminatedReason();
                            if (reason != null && reason.equalsIgnoreCase("Completed")) {
                                rs.message("Container exited successfully but it will restart due to the application's restart policy. If this is unexpected, review logs or adjust the container command/restart policy.");
                            } else {
                                rs.message("Container crashed" + (StringUtils.isNotBlank(reason) ? " (reason: " + reason + ")" : "") + ". Fix the issue, then re-publish.");
                            }
                            if (StringUtils.isNotBlank(cs.getLastStageTerminatedMessage())) {
                                rs.details(Collections.singletonList(prettifyContainerStatusTerminatedMessage(cs.getLastStageTerminatedMessage())));
                            }
                            continue; // skip other enrichment
                        } else if (ResourceStatus.ContainerStatusStage.WAITING.equals(cs.getLastStage())) {
                            if (StringUtils.isBlank(rs.getMessage())) {
                                rs.message("Application is deploying");
                            }
                        }
                    }

                    List<String> waitingMsgs = rs.getContainerStatuses().stream()
                        .map(ResourceStatus.ContainerStatus::getWaitingMessage)
                        .filter(Objects::nonNull)
                        .toList();
                    if (!waitingMsgs.isEmpty()) {
                        rs.details(waitingMsgs);
                        if (StringUtils.isBlank(rs.getMessage())) {
                            rs.message("One or more containers are waiting: see details for reasons.");
                        }
                    }
                }
            } else if (ApplicationStatus.DEPLOYED.equals(application.getStatus())) {
                if (rs.getStage() == ResourceStatus.ResourceStatusStage.RESTARTING) {
                    if (StringUtils.isBlank(rs.getMessage())) {
                        rs.message("Restarting (crash recovery). Investigate logs if restarts continue.");
                    }
                } else if (rs.getStage() == ResourceStatus.ResourceStatusStage.DEGRADED) {
                    if (StringUtils.isBlank(rs.getMessage())) {
                        rs.message("Degraded: at least one container not Ready or restarting; service may be partially available.");
                    }
                }
            }
        }

        // Aggregate overall stage across pods (worst-first ordering)
        boolean anyFailed = resourceStatuses.stream().anyMatch(r -> r != null && r.getStage() == ResourceStatus.ResourceStatusStage.FAILED);
        boolean anyRestarting = resourceStatuses.stream().anyMatch(r -> r != null && r.getStage() == ResourceStatus.ResourceStatusStage.RESTARTING);
        boolean anyDegraded = resourceStatuses.stream().anyMatch(r -> r != null && r.getStage() == ResourceStatus.ResourceStatusStage.DEGRADED);
        boolean anyInitializing = resourceStatuses.stream().anyMatch(r -> r != null && r.getStage() == ResourceStatus.ResourceStatusStage.INITIALIZING);
        boolean anyWaiting = resourceStatuses.stream().anyMatch(r -> r != null && r.getStage() == ResourceStatus.ResourceStatusStage.WAITING);
        boolean allRunning = resourceStatuses.stream().allMatch(r -> r != null && r.getStage() == ResourceStatus.ResourceStatusStage.RUNNING);
        boolean allCompleted = resourceStatuses.stream().allMatch(r -> r != null && r.getStage() == ResourceStatus.ResourceStatusStage.COMPLETED);
        boolean allRunningOrCompleted = resourceStatuses.stream().allMatch(r -> r != null && (r.getStage() == ResourceStatus.ResourceStatusStage.RUNNING || r.getStage() == ResourceStatus.ResourceStatusStage.COMPLETED));
        boolean allStopped = resourceStatuses.stream().allMatch(r -> r != null && r.getStage() == ResourceStatus.ResourceStatusStage.STOPPED);

        ResourceStatus.ResourceStatusStage aggregated;

        if (anyFailed) aggregated = ResourceStatus.ResourceStatusStage.FAILED;
        else if (anyRestarting) aggregated = ResourceStatus.ResourceStatusStage.RESTARTING;
        else if (anyDegraded) aggregated = ResourceStatus.ResourceStatusStage.DEGRADED;
        else if (anyInitializing) aggregated = ResourceStatus.ResourceStatusStage.INITIALIZING;
        else if (anyWaiting) aggregated = ResourceStatus.ResourceStatusStage.WAITING;
        else if (allCompleted) aggregated = ResourceStatus.ResourceStatusStage.COMPLETED;
        else if (allRunning) aggregated = ResourceStatus.ResourceStatusStage.RUNNING;
        else if (allRunningOrCompleted) aggregated = ResourceStatus.ResourceStatusStage.RUNNING;
        else if (allStopped) aggregated = ResourceStatus.ResourceStatusStage.STOPPED;
        else aggregated = ResourceStatus.ResourceStatusStage.UNKNOWN;

        log.trace("oldStatus={} anyFailed={} anyRestarting={} anyDegraded={} anyInitializing={} anyWaiting={} allRunning={} allCompleted={} allStopped={} => aggregated={}",
            application.getStatus().name(), anyFailed, anyRestarting, anyDegraded, anyInitializing, anyWaiting, allRunning, allCompleted, allStopped, aggregated);

        // Application status transitions rely on aggregated stage
        ApplicationStatus current = application.getStatus();
        switch (aggregated) {
            case FAILED -> {
                if (!ApplicationStatus.ERROR.equals(current)) {
                    log.trace("Transitioning application {} status from {} to ERROR based on resource status {}",
                        application.getId(), current, aggregated);
                    application.setStatus(ApplicationStatus.ERROR);
                    if (application.getId() != null) {
                        applicationService.updateStatus(application.getId(), ApplicationStatus.ERROR);
                    }
                }
            }
            case RUNNING, COMPLETED -> {
                if (!ApplicationStatus.DEPLOYED.equals(current)) {
                    log.trace("Transitioning application {} status from {} to DEPLOYED based on resource status {}",
                        application.getId(), current, aggregated);
                    application.setStatus(ApplicationStatus.DEPLOYED);
                    if (application.getId() != null) {
                        applicationService.updateStatus(application.getId(), ApplicationStatus.DEPLOYED);
                    }
                }
            }
            case INITIALIZING, WAITING, RESTARTING, DEGRADED, UNKNOWN -> {
                if (current == ApplicationStatus.CREATED || current == ApplicationStatus.INITIALIZED) {
                    log.trace("Transitioning application {} status from {} to DEPLOYING based on resource status {}",
                        application.getId(), current, aggregated);
                    application.setStatus(ApplicationStatus.DEPLOYING);
                    if (application.getId() != null) {
                        applicationService.updateStatus(application.getId(), ApplicationStatus.DEPLOYING);
                    }
                } else if (current == ApplicationStatus.ERROR) {
                    // Allow recovery from ERROR when pods are no longer failed
                    log.trace("Transitioning application {} status from {} to DEPLOYING based on resource status {}",
                        application.getId(), current, aggregated);
                    application.setStatus(ApplicationStatus.DEPLOYING);
                    if (application.getId() != null) {
                        applicationService.updateStatus(application.getId(), ApplicationStatus.DEPLOYING);
                    }
                }
            }
            case STOPPED -> {
                if (current != ApplicationStatus.INITIALIZED) {
                    log.trace("Transitioning application {} status from {} to INITIALIZED based on resource status {}",
                        application.getId(), current, aggregated);
                    application.setStatus(ApplicationStatus.INITIALIZED);
                    if (application.getId() != null) {
                        applicationService.updateStatus(application.getId(), ApplicationStatus.INITIALIZED);
                    }
                }
            }
        }

        log.trace("newStatus={} anyFailed={} anyRestarting={} anyDegraded={} anyInitializing={} anyWaiting={} allRunning={} allCompleted={} allStopped={} => aggregated={}",
            application.getStatus().name(), anyFailed, anyRestarting, anyDegraded, anyInitializing, anyWaiting, allRunning, allCompleted, allStopped, aggregated);

        return resourceStatuses;
    }

    protected static String pendingContainers(List<ResourceStatus.ContainerStatus> containerStatuses) {
        return containerStatuses.stream()
            .filter(status -> !ResourceStatus.ContainerStatusStage.RUNNING.equals(status.getStage()))
            .map(ResourceStatus.ContainerStatus::getContainerName).collect(Collectors.joining(", "));
    }

    protected String getMessage(String key) {
        return messageSource.getMessage(key, null, Locale.getDefault());
    }

    private String prettifyContainerStatusTerminatedMessage(String message) {
        if (message == null) {
            return null;
        }

        message = message.replace("failed to create containerd task:", "").trim();
        message = message.replace("OCI runtime create failed:", "").trim();
        message = message.replace("failed to create shim task:", "").trim();
        message = message.replace("runc create failed:", "").trim();

        return message;
    }

    @Data
    @Builder
    static class AppStatus {
        String applicationId;
        ApplicationStatus status;
        List<ResourceStatus> resourceStatus;
    }
}
