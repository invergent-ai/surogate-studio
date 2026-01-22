package net.statemesh.web.rest.k8s;

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
public class ControlResource {
    private final Logger log = LoggerFactory.getLogger(ControlResource.class);
    private final ResourceControlService resourceControlService;

    public ControlResource(ResourceControlService resourceControlService) {
        this.resourceControlService = resourceControlService;
    }

    @PostMapping("/start/{applicationId}")
    public ResponseEntity<Boolean> start(
        @PathVariable(name = "applicationId") String applicationId,
        @RequestParam(required = false, name = "component") String component) {
        log.debug("REST request to start application {}, component {}", applicationId, component);
        return ResponseEntity.ok(
            resourceControlService.startApplication(applicationId, component)
        );
    }

    @PostMapping("/stop/{applicationId}")
    public ResponseEntity<Boolean> stop(
        @PathVariable(name = "applicationId") String applicationId,
        @RequestParam(required = false, name = "component") String component) {
        log.debug("REST request to stop application {}, component {}", applicationId, component);
        return ResponseEntity.ok(
            resourceControlService.stopApplication(applicationId, component)
        );
    }

    @PostMapping("/restart/{applicationId}")
    public ResponseEntity<Boolean> restart(
        @PathVariable(name = "applicationId") String applicationId,
        @RequestParam(required = false, name = "component") String component) {
        log.debug("REST request to restart application {}, component {}", applicationId, component);
        return ResponseEntity.ok(
            resourceControlService.restartApplication(applicationId, component)
        );
    }

    @PostMapping("/scale/{applicationId}/{replicas}")
    public ResponseEntity<Boolean> scale(
        @PathVariable(name = "applicationId") String applicationId,
        @PathVariable(name = "replicas") Integer replicas) {
        log.debug("REST request to scale application {}", applicationId);
        return ResponseEntity.ok(
            resourceControlService.scaleApplication(applicationId, replicas)
        );
    }

    @PostMapping("/startdb/{databaseId}")
    public ResponseEntity<Boolean> startDatabase(@PathVariable(name = "databaseId") String databaseId) {
        log.debug("REST request to start database {}", databaseId);
        return ResponseEntity.ok(
            resourceControlService.startDatabase(databaseId)
        );
    }

    @PostMapping("/stopdb/{databaseId}")
    public ResponseEntity<Boolean> stopDatabase(@PathVariable(name = "databaseId") String databaseId) {
        log.debug("REST request to stop database {}", databaseId);
        return ResponseEntity.ok(
            resourceControlService.stopDatabase(databaseId)
        );
    }
}
