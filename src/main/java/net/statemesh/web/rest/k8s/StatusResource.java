package net.statemesh.web.rest.k8s;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.service.k8s.status.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

@Controller
@RequestMapping("/api/status")
@Tag(name = "Status", description = "Resource status streaming for apps, databases, models, and jobs")
public class StatusResource {
    private final Logger log = LoggerFactory.getLogger(StatusResource.class);
    private final AppStatusService appStatusService;
    private final ModelStatusService modelStatusService;
    private final DatabaseStatusService databaseStatusService;
    private final TaskRunStatusService taskRunStatusService;
    private final RayJobStatusService rayJobStatusService;
    private final ApplicationProperties applicationProperties;

    public StatusResource(
        @Qualifier("appStatusService") AppStatusService appStatusService,
        @Qualifier("modelStatusService") ModelStatusService modelStatusService,
        DatabaseStatusService databaseStatusService,
        TaskRunStatusService taskRunStatusService,
        RayJobStatusService rayJobStatusService,
        ApplicationProperties applicationProperties) {
        this.appStatusService = appStatusService;
        this.modelStatusService = modelStatusService;
        this.databaseStatusService = databaseStatusService;
        this.taskRunStatusService = taskRunStatusService;
        this.rayJobStatusService = rayJobStatusService;
        this.applicationProperties = applicationProperties;
    }

    @Operation(summary = "Start application status stream")
    @ApiResponse(responseCode = "200", description = "Status stream started")
    @GetMapping(value = "/app/{applicationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startAppStatus(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId) {
        log.debug("REST request to start status (SSE) for application {}", applicationId);
        appStatusService.start(applicationProperties.getMetrics().getStatusPollInterval(),
            applicationProperties.getMetrics().getStatusWaitTimeout(),
            applicationId);
        return appStatusService.registerStatusEmitter(applicationId);
    }

    @Operation(summary = "Stop application status stream")
    @ApiResponse(responseCode = "200", description = "Status stream stopped")
    @DeleteMapping("/app/{applicationId}")
    public ResponseEntity<Void> stopAppStatus(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId) {
        appStatusService.stop(applicationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Start database status polling")
    @ApiResponse(responseCode = "200", description = "Database status polling started")
    @PostMapping("/db/{databaseId}")
    public ResponseEntity<Void> startDatabaseStatus(
        @Parameter(description = "Database ID") @PathVariable(name = "databaseId") String databaseId,
        Principal principal) {
        log.debug("REST request to start status for database {}", databaseId);
        databaseStatusService.startDatabaseStatus(databaseId, principal.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Stream database status via SSE")
    @ApiResponse(responseCode = "200", description = "Database status stream started")
    @GetMapping(value = "/db/{databaseId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDatabaseStatus(
        @Parameter(description = "Database ID") @PathVariable(name = "databaseId") String databaseId) {
        return databaseStatusService.registerStatusEmitter(databaseId);
    }

    @Operation(summary = "Stop database status polling")
    @ApiResponse(responseCode = "200", description = "Database status polling stopped")
    @DeleteMapping("/db/{databaseId}")
    public ResponseEntity<Void> stopDatabaseStatus(
        @Parameter(description = "Database ID") @PathVariable(name = "databaseId") String databaseId) {
        databaseStatusService.stopStatus(databaseId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Start model status stream")
    @ApiResponse(responseCode = "200", description = "Model status stream started")
    @GetMapping(value = "/model/{applicationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startModelStatus(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId) {
        log.debug("REST request to start status for model {}", applicationId);
        modelStatusService.start(applicationProperties.getMetrics().getStatusPollInterval(),
            applicationProperties.getMetrics().getStatusWaitTimeout(),
            applicationId);
        return modelStatusService.registerStatusEmitter(applicationId);
    }

    @Operation(summary = "Stop model status stream")
    @ApiResponse(responseCode = "200", description = "Model status stream stopped")
    @DeleteMapping("/model/{applicationId}")
    public ResponseEntity<Void> stopModelStatus(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId) {
        modelStatusService.stop(applicationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Start task run status stream")
    @ApiResponse(responseCode = "200", description = "Task run status stream started")
    @GetMapping(value = "/task-run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startTaskRunStatus(
        @Parameter(description = "Comma-separated task IDs") @RequestParam("ids") String ids) {
        log.trace("REST request to start status for tasks {}", ids);
        var taskIds = ids.split(",");
        taskRunStatusService.start(applicationProperties.getMetrics().getStatusPollInterval(),
            applicationProperties.getMetrics().getStatusWaitTimeout(), taskIds);
        return taskRunStatusService.registerStatusEmitter(taskIds);
    }

    @Operation(summary = "Start Ray job status stream")
    @ApiResponse(responseCode = "200", description = "Ray job status stream started")
    @GetMapping(value = "/ray-job", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startRayJobStatus(
        @Parameter(description = "Comma-separated job IDs") @RequestParam("ids") String ids) {
        log.trace("REST request to start status for ray jobs {}", ids);
        var jobIds = ids.split(",");
        rayJobStatusService.start(applicationProperties.getMetrics().getStatusPollInterval(),
            applicationProperties.getMetrics().getStatusWaitTimeout(), jobIds);
        return rayJobStatusService.registerStatusEmitter(jobIds);
    }

    @Operation(summary = "Stop task run status stream")
    @ApiResponse(responseCode = "200", description = "Task run status stream stopped")
    @DeleteMapping("/task-run")
    public ResponseEntity<Void> stopTaskRunStatus(
        @Parameter(description = "Comma-separated task IDs") @RequestParam("ids") String ids) {
        var taskIds = ids.split(",");
        taskRunStatusService.stop(taskIds);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Stop Ray job status stream")
    @ApiResponse(responseCode = "200", description = "Ray job status stream stopped")
    @DeleteMapping("/ray-job")
    public ResponseEntity<Void> stopRayJobStatus(
        @Parameter(description = "Comma-separated job IDs") @RequestParam("ids") String ids) {
        var jobIds = ids.split(",");
        rayJobStatusService.stop(jobIds);
        return ResponseEntity.ok().build();
    }
}
