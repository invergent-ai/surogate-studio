package net.statemesh.web.rest.k8s;

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

    @GetMapping(value = "/app/{applicationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startAppStatus(
        @PathVariable(name = "applicationId") String applicationId) {
        log.debug("REST request to start status (SSE) for application {}", applicationId);
        appStatusService.start(applicationProperties.getMetrics().getStatusPollInterval(),
            applicationProperties.getMetrics().getStatusWaitTimeout(),
            applicationId);
        return appStatusService.registerStatusEmitter(applicationId);
    }

    @DeleteMapping("/app/{applicationId}")
    public ResponseEntity<Void> stopAppStatus(@PathVariable(name = "applicationId") String applicationId) {
        appStatusService.stop(applicationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/db/{databaseId}")
    public ResponseEntity<Void> startDatabaseStatus(
        @PathVariable(name = "databaseId") String databaseId,
        Principal principal) {
        log.debug("REST request to start status for database {}", databaseId);
        databaseStatusService.startDatabaseStatus(databaseId, principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/db/{databaseId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDatabaseStatus(
        @PathVariable(name = "databaseId") String databaseId) {
        return databaseStatusService.registerStatusEmitter(databaseId);
    }

    @DeleteMapping("/db/{databaseId}")
    public ResponseEntity<Void> stopDatabaseStatus(@PathVariable(name = "databaseId") String databaseId) {
        databaseStatusService.stopStatus(databaseId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/model/{applicationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startModelStatus(
        @PathVariable(name = "applicationId") String applicationId) {
        log.debug("REST request to start status for model {}", applicationId);

        modelStatusService.start(applicationProperties.getMetrics().getStatusPollInterval(),
            applicationProperties.getMetrics().getStatusWaitTimeout(),
            applicationId);
        return modelStatusService.registerStatusEmitter(applicationId);
    }

    @DeleteMapping("/model/{applicationId}")
    public ResponseEntity<Void> stopModelStatus(
        @PathVariable(name = "applicationId") String applicationId) {
        modelStatusService.stop(applicationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/task-run",  produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startTaskRunStatus(@RequestParam("ids") String ids) {
        log.trace("REST request to start status for tasks {}", ids);
        var taskIds = ids.split(",");
        taskRunStatusService.start(applicationProperties.getMetrics().getStatusPollInterval(),
            applicationProperties.getMetrics().getStatusWaitTimeout(), taskIds);
        return taskRunStatusService.registerStatusEmitter(taskIds);
    }

    @GetMapping(value = "/ray-job",  produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startRayJobStatus(@RequestParam("ids") String ids) {
        log.trace("REST request to start status for ray jobs {}", ids);
        var jobIds = ids.split(",");
        rayJobStatusService.start(applicationProperties.getMetrics().getStatusPollInterval(),
            applicationProperties.getMetrics().getStatusWaitTimeout(), jobIds);
        return rayJobStatusService.registerStatusEmitter(jobIds);
    }

    @DeleteMapping("/task-run")
    public ResponseEntity<Void> stopTaskRunStatus(
        @RequestParam("ids") String ids) {
        var taskIds = ids.split(",");
        taskRunStatusService.stop(taskIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/ray-job")
    public ResponseEntity<Void> stopRayJobStatus(
        @RequestParam("ids") String ids) {
        var jobIds = ids.split(",");
        rayJobStatusService.stop(jobIds);
        return ResponseEntity.ok().build();
    }
}
