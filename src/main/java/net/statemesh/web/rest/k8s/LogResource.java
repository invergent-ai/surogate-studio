package net.statemesh.web.rest.k8s;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.service.k8s.LogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogResource {
    private final LogService logService;
    private final ApplicationProperties applicationProperties;

    @GetMapping(value = "/logs/{resourceType}/{resourceId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startResourceLogs(
        @PathVariable(name = "resourceId") String resourceId,
        @PathVariable(name = "resourceType") String resourceType,
        @RequestParam(name = "podName") String podName,
        @RequestParam(required = false, name = "containerId") String containerId,
        @RequestParam(required = false, name = "limit") Integer limit,
        @RequestParam(required = false, name = "sinceSeconds") Integer sinceSeconds
    ) {
        var emId = new String[]{resourceType, resourceId, podName, containerId};
        logService.start(limit, sinceSeconds, applicationProperties.getMetrics().getStatusPollInterval(),
            applicationProperties.getMetrics().getLogsWatchTimeout(), emId);

        return logService.registerStatusEmitter(emId);
    }

    @DeleteMapping("/logs/{resourceType}/{resourceId}")
    public ResponseEntity<Void> stopAppStatus(
        @PathVariable(name = "resourceType") String resourceType,
        @PathVariable(name = "resourceId") String resourceId,
        @RequestParam(name = "podName") String podName,
        @RequestParam(required = false, name = "containerId") String containerId
    ) {
        var emId = new String[]{resourceType, resourceId, podName, containerId};
        logService.stop(emId);
        return ResponseEntity.ok().build();
    }
}
