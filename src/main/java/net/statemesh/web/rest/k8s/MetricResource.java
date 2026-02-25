package net.statemesh.web.rest.k8s;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.service.k8s.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/metrics")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Application, GPU, and model metrics streaming")
public class MetricResource {
    private final ApplicationProperties applicationProperties;
    private final MetricService metricService;
    private final GpuMetricService gpuMetricService;
    private final ModelRouterMetricService modelRouterMetricService;
    private final ModelWorkerMetricService modelWorkerMetricService;
    private final RayJobMetricService rayJobMetricService;

    @Operation(summary = "Start application metrics stream", description = "Start SSE stream for application container metrics")
    @ApiResponse(responseCode = "200", description = "Metrics stream started")
    @GetMapping(value = "/metrics/{applicationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startMetrics(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container ID") @RequestParam(required = false, name = "containerId") String containerId) {
        log.debug("REST request to start metrics for application {} and container {}", applicationId, containerId);
        metricService.start(applicationProperties.getMetrics().getMetricsPollInterval(),
            applicationProperties.getMetrics().getMetricsWaitTimeout(),
            applicationId, podName, containerId);
        return metricService.registerStatusEmitter(applicationId, podName, containerId);
    }

    @Operation(summary = "Stop application metrics stream")
    @ApiResponse(responseCode = "200", description = "Metrics stream stopped")
    @DeleteMapping("/metrics/{applicationId}")
    public void stopMetrics(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container ID") @RequestParam(required = false, name = "containerId") String containerId) {
        metricService.stop(applicationId, podName, containerId);
    }

    @Operation(summary = "Start GPU metrics stream", description = "Start SSE stream for GPU metrics on a specific node")
    @ApiResponse(responseCode = "200", description = "GPU metrics stream started")
    @GetMapping(value = "/gpu-metrics/{nodeId}/{gpuId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startGpuMetrics(
        @Parameter(description = "Node ID") @PathVariable(name = "nodeId") String nodeId,
        @Parameter(description = "GPU ID") @PathVariable(name = "gpuId") String gpuId) {
        log.debug("REST request to start GPU metrics for node {} and gpu {}", nodeId, gpuId);
        gpuMetricService.start(applicationProperties.getMetrics().getMetricsPollInterval(),
            applicationProperties.getMetrics().getMetricsWaitTimeout(),
            nodeId, gpuId);
        return gpuMetricService.registerStatusEmitter(nodeId, gpuId);
    }

    @Operation(summary = "Stop GPU metrics stream")
    @ApiResponse(responseCode = "200", description = "GPU metrics stream stopped")
    @DeleteMapping("/gpu-metrics/{nodeId}/{gpuId}")
    public void stopGpuMetrics(
        @Parameter(description = "Node ID") @PathVariable(name = "nodeId") String nodeId,
        @Parameter(description = "GPU ID") @PathVariable(name = "gpuId") String gpuId) {
        gpuMetricService.stop(nodeId, gpuId);
    }

    @Operation(summary = "Start model router metrics stream")
    @ApiResponse(responseCode = "200", description = "Model router metrics stream started")
    @GetMapping(value = "/model-router-metrics/{applicationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startModelRouterMetrics(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container name") @RequestParam(name = "containerName") String containerName) {
        log.debug("applicationId: {}, podName: {}, containerName: {}", applicationId, podName, containerName);
        modelRouterMetricService.start(applicationProperties.getMetrics().getMetricsPollInterval(),
            applicationProperties.getMetrics().getMetricsWaitTimeout(),
            applicationId, podName, containerName);
        return modelRouterMetricService.registerStatusEmitter(applicationId, podName, containerName);
    }

    @Operation(summary = "Stop model router metrics stream")
    @ApiResponse(responseCode = "200", description = "Model router metrics stream stopped")
    @DeleteMapping("/model-router-metrics/{applicationId}")
    public void stopModelRouterMetrics(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container name") @RequestParam(name = "containerName") String containerName) {
        log.debug("REST request to stop model router metrics for application {} and container {}", applicationId, containerName);
        modelRouterMetricService.stop(applicationId, podName, containerName);
    }

    @Operation(summary = "Start model worker metrics stream")
    @ApiResponse(responseCode = "200", description = "Model worker metrics stream started")
    @GetMapping(value = "/model-worker-metrics/{applicationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startModelWorkerMetrics(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container name") @RequestParam(name = "containerName") String containerName) {
        log.debug("REST request to start model worker metrics for application {} and container {}", applicationId, containerName);
        modelWorkerMetricService.start(applicationProperties.getMetrics().getMetricsPollInterval(),
            applicationProperties.getMetrics().getMetricsWaitTimeout(), applicationId, podName, containerName);
        return modelWorkerMetricService.registerStatusEmitter(applicationId, podName, containerName);
    }

    @Operation(summary = "Stop model worker metrics stream")
    @ApiResponse(responseCode = "200", description = "Model worker metrics stream stopped")
    @DeleteMapping("/model-worker-metrics/{applicationId}")
    public void stopModelWorkerMetrics(
        @Parameter(description = "Application ID") @PathVariable(name = "applicationId") String applicationId,
        @Parameter(description = "Pod name") @RequestParam(name = "podName") String podName,
        @Parameter(description = "Container name") @RequestParam(name = "containerName") String containerName) {
        log.debug("REST request to stop model worker metrics for application {} and container {}", applicationId, containerName);
        modelWorkerMetricService.stop(applicationId, podName, containerName);
    }

    @Operation(summary = "Start Ray job metrics stream")
    @ApiResponse(responseCode = "200", description = "Ray job metrics stream started")
    @GetMapping(value = "/ray-job-metrics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startRayJobMetrics(
        @Parameter(description = "Comma-separated job IDs") @RequestParam("ids") String ids) {
        log.trace("REST request to start metrics for ray jobs {}", ids);
        var jobIds = ids.split(",");
        rayJobMetricService.start(applicationProperties.getMetrics().getMetricsPollInterval(),
            applicationProperties.getMetrics().getMetricsWaitTimeout(), jobIds);
        return rayJobMetricService.registerStatusEmitter(jobIds);
    }

    @Operation(summary = "Stop Ray job metrics stream")
    @ApiResponse(responseCode = "200", description = "Ray job metrics stream stopped")
    @DeleteMapping("/ray-job-metrics")
    public void stopRayJobMetrics(
        @Parameter(description = "Comma-separated job IDs") @RequestParam("ids") String ids) {
        var jobIds = ids.split(",");
        rayJobMetricService.stop(jobIds);
    }
}
