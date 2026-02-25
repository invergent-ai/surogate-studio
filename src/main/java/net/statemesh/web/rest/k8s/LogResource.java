package net.statemesh.web.rest.k8s;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.service.dto.LogDTO;
import net.statemesh.service.k8s.LogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.TimeoutException;

@Controller
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Logs", description = "Resource log streaming and history")
public class LogResource {
    private final LogService logService;
    private final ApplicationProperties applicationProperties;

    @Operation(summary = "Start log stream", description = "Start SSE stream for resource logs")
    @ApiResponse(responseCode = "200", description = "Log stream started")
    @GetMapping(value = "/logs/{resourceType}/{resourceId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startResourceLogs(
        @Parameter(description = "Resource ID") @PathVariable(name = "resourceId") String resourceId,
        @Parameter(description = "Resource type") @PathVariable(name = "resourceType") String resourceType,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container ID") @RequestParam(required = false, name = "containerId") String containerId,
        @Parameter(description = "Log line limit") @RequestParam(required = false, name = "limit") Integer limit,
        @Parameter(description = "Fetch logs since N seconds ago") @RequestParam(required = false, name = "sinceSeconds") Integer sinceSeconds
    ) {
        var emId = new String[]{resourceType, resourceId, podName, containerId};
        logService.start(limit, sinceSeconds, applicationProperties.getMetrics().getStatusPollInterval(),
            applicationProperties.getMetrics().getLogsWatchTimeout(), emId);
        return logService.registerStatusEmitter(emId);
    }

    @Operation(summary = "Stop log stream")
    @ApiResponse(responseCode = "200", description = "Log stream stopped")
    @DeleteMapping("/logs/{resourceType}/{resourceId}")
    public ResponseEntity<Void> stopAppStatus(
        @Parameter(description = "Resource type") @PathVariable(name = "resourceType") String resourceType,
        @Parameter(description = "Resource ID") @PathVariable(name = "resourceId") String resourceId,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container ID") @RequestParam(required = false, name = "containerId") String containerId
    ) {
        var emId = new String[]{resourceType, resourceId, podName, containerId};
        logService.stop(emId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Fetch log history", description = "Fetch historical logs for a resource")
    @ApiResponse(responseCode = "200", description = "Log history returned")
    @ApiResponse(responseCode = "500", description = "Failed to fetch log history")
    @GetMapping("/logs/{resourceType}/{resourceId}/history")
    public ResponseEntity<List<LogDTO>> fetchLogHistory(
        @Parameter(description = "Resource type") @PathVariable(name = "resourceType") String resourceType,
        @Parameter(description = "Resource ID") @PathVariable(name = "resourceId") String resourceId,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container ID") @RequestParam(required = false, name = "containerId") String containerId,
        @Parameter(description = "Log line limit") @RequestParam(required = false, name = "limit") Integer limit,
        @Parameter(description = "Fetch logs since N seconds ago") @RequestParam(required = false, name = "sinceSeconds") Integer sinceSeconds,
        @Parameter(description = "Number of tail lines") @RequestParam(required = false, name = "tailLines") Integer tailLines
    ) {
        try {
            List<LogDTO> logs = logService.fetchHistory(
                resourceType, resourceId, podName, containerId, limit, sinceSeconds, tailLines);
            return ResponseEntity.ok(logs);
        } catch (TimeoutException | InterruptedException e) {
            log.error("Failed to fetch log history for {}/{}", resourceType, resourceId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
