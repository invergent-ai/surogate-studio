package net.statemesh.web.rest.k8s;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.statemesh.service.k8s.ResourceControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/control")
@Tag(name = "Resource Control", description = "Application and database lifecycle management")
public class ControlResource {
    private final Logger log = LoggerFactory.getLogger(ControlResource.class);
    private final ResourceControlService resourceControlService;

    public ControlResource(ResourceControlService resourceControlService) {
        this.resourceControlService = resourceControlService;
    }

    @Operation(summary = "Start application", description = "Start an application or a specific component")
    @ApiResponse(responseCode = "200", description = "Application started successfully")
    @PostMapping("/start/{applicationId}")
    public ResponseEntity<Boolean> start(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Specific component to start") @RequestParam(required = false, name = "component") String component) {
        log.debug("REST request to start application {}, component {}", applicationId, component);
        return ResponseEntity.ok(
            resourceControlService.startApplication(applicationId, component)
        );
    }

    @Operation(summary = "Stop application", description = "Stop an application or a specific component")
    @ApiResponse(responseCode = "200", description = "Application stopped successfully")
    @PostMapping("/stop/{applicationId}")
    public ResponseEntity<Boolean> stop(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Specific component to stop") @RequestParam(required = false, name = "component") String component) {
        log.debug("REST request to stop application {}, component {}", applicationId, component);
        return ResponseEntity.ok(
            resourceControlService.stopApplication(applicationId, component)
        );
    }

    @Operation(summary = "Restart application", description = "Restart an application or a specific component")
    @ApiResponse(responseCode = "200", description = "Application restarted successfully")
    @PostMapping("/restart/{applicationId}")
    public ResponseEntity<Boolean> restart(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Specific component to restart") @RequestParam(required = false, name = "component") String component) {
        log.debug("REST request to restart application {}, component {}", applicationId, component);
        return ResponseEntity.ok(
            resourceControlService.restartApplication(applicationId, component)
        );
    }

    @Operation(summary = "Scale application", description = "Scale an application to a specified number of replicas")
    @ApiResponse(responseCode = "200", description = "Application scaled successfully")
    @PostMapping("/scale/{applicationId}/{replicas}")
    public ResponseEntity<Boolean> scale(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Number of replicas", example = "3") @PathVariable(name = "replicas") Integer replicas) {
        log.debug("REST request to scale application {}", applicationId);
        return ResponseEntity.ok(
            resourceControlService.scaleApplication(applicationId, replicas)
        );
    }

    @Operation(summary = "Start database", description = "Start a database instance")
    @ApiResponse(responseCode = "200", description = "Database started successfully")
    @PostMapping("/startdb/{databaseId}")
    public ResponseEntity<Boolean> startDatabase(
        @Parameter(description = "Database ID") @PathVariable(name = "databaseId") String databaseId) {
        log.debug("REST request to start database {}", databaseId);
        return ResponseEntity.ok(
            resourceControlService.startDatabase(databaseId)
        );
    }

    @Operation(summary = "Stop database", description = "Stop a database instance")
    @ApiResponse(responseCode = "200", description = "Database stopped successfully")
    @PostMapping("/stopdb/{databaseId}")
    public ResponseEntity<Boolean> stopDatabase(
        @Parameter(description = "Database ID") @PathVariable(name = "databaseId") String databaseId) {
        log.debug("REST request to stop database {}", databaseId);
        return ResponseEntity.ok(
            resourceControlService.stopDatabase(databaseId)
        );
    }
}
