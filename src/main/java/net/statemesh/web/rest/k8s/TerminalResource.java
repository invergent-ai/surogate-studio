package net.statemesh.web.rest.k8s;

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
public class TerminalResource {
    private final Logger log = LoggerFactory.getLogger(TerminalResource.class);
    private final TerminalService terminalService;

    @GetMapping("/app/{applicationId}")
    public ResponseEntity<Void> startAppTerminal(
        @PathVariable(name = "applicationId") String applicationId,
        @RequestParam(name = "podName") String podName,
        @RequestParam(required = false, name = "containerId") String containerId) {
        log.debug("REST request to start terminal for application {}, pod {}, container {}", applicationId, podName, containerId);
        terminalService.startTerminal(ControlTask.ControlObject.APPLICATION, applicationId, podName, containerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/app/{applicationId}")
    public ResponseEntity<Void> stopAppTerminal(@PathVariable(name = "applicationId") String applicationId,
                                                @RequestParam(name = "podName") String podName,
                                                @RequestParam(required = false, name = "containerId") String containerId) {
        terminalService.stopTerminal(applicationId, podName, containerId);
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/terminal")
    public void sendAppTerminalCommand(@Payload LineDTO commandLine) {
        terminalService.enqueueTerminalCommand(commandLine);
    }
}
