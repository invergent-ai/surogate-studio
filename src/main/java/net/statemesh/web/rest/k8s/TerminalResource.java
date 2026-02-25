package net.statemesh.web.rest.k8s;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.statemesh.k8s.task.control.ControlTask;
import net.statemesh.service.dto.LineDTO;
import net.statemesh.service.k8s.TerminalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/terminal")
@RequiredArgsConstructor
@Tag(name = "Terminal", description = "Interactive terminal sessions for application containers")
public class TerminalResource {
    private final Logger log = LoggerFactory.getLogger(TerminalResource.class);
    private final TerminalService terminalService;

    @Operation(summary = "Start terminal session", description = "Start an interactive terminal for an application container")
    @ApiResponse(responseCode = "200", description = "Terminal session started")
    @GetMapping("/app/{applicationId}")
    public ResponseEntity<Void> startAppTerminal(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container ID") @RequestParam(required = false, name = "containerId") String containerId) {
        log.debug("REST request to start terminal for application {}, pod {}, container {}", applicationId, podName, containerId);
        terminalService.startTerminal(ControlTask.ControlObject.APPLICATION, applicationId, podName, containerId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Stop terminal session")
    @ApiResponse(responseCode = "200", description = "Terminal session stopped")
    @DeleteMapping("/app/{applicationId}")
    public ResponseEntity<Void> stopAppTerminal(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container ID") @RequestParam(required = false, name = "containerId") String containerId) {
        terminalService.stopTerminal(applicationId, podName, containerId);
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/terminal")
    public void sendAppTerminalCommand(@Payload LineDTO commandLine) {
        terminalService.enqueueTerminalCommand(commandLine);
    }
}
