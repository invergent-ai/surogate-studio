package net.statemesh.web.rest.k8s;

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
public class MetricResource {
    private final ApplicationProperties applicationProperties;
    private final MetricService metricService;
    private final GpuMetricService gpuMetricService;
    private final ModelRouterMetricService modelRouterMetricService;
    private final ModelWorkerMetricService modelWorkerMetricService;
    private final RayJobMetricService rayJobMetricService;

    @GetMapping(value = "/metrics/{applicationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startMetrics(
        @PathVariable(name = "applicationId") String applicationId,
        @RequestParam(name = "podName") String podName,
        @RequestParam(required = false, name = "containerId") String containerId) {
        log.debug("REST request to start metrics for application {} and container {}", applicationId, containerId);
        metricService.start(applicationProperties.getMetrics().getMetricsPollInterval(),
            applicationProperties.getMetrics().getMetricsWaitTimeout(),
            applicationId, podName, containerId);
        return metricService.registerStatusEmitter(applicationId, podName, containerId);
    }

    @DeleteMapping("/metrics/{applicationId}")
    public void stopMetrics(@PathVariable(name = "applicationId") String applicationId,
                            @RequestParam(name = "podName") String podName,
                            @RequestParam(required = false, name = "containerId") String containerId) {
        metricService.stop(applicationId, podName, containerId);
    }

    @GetMapping(value = "/gpu-metrics/{nodeId}/{gpuId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startGpuMetrics(@PathVariable(name = "nodeId") String nodeId, @PathVariable(name = "gpuId") String gpuId) {
        log.debug("REST request to start GPU metrics for node {} and gpu {}", nodeId, gpuId);
        gpuMetricService.start(applicationProperties.getMetrics().getMetricsPollInterval(),
            applicationProperties.getMetrics().getMetricsWaitTimeout(),
            nodeId, gpuId);
        return gpuMetricService.registerStatusEmitter(nodeId, gpuId);
    }

    @DeleteMapping("/gpu-metrics/{nodeId}/{gpuId}")
    public void stopGpuMetrics(@PathVariable(name = "nodeId") String nodeId, @PathVariable(name = "gpuId") String gpuId) {
        gpuMetricService.stop(nodeId, gpuId);
    }

    @GetMapping(value = "/model-router-metrics/{applicationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startModelRouterMetrics(
        @PathVariable(name = "applicationId") String applicationId,
        @RequestParam(name = "podName") String podName,
        @RequestParam(name = "containerName") String containerName) {
        log.debug("applicationId: {}, podName: {}, containerName: {}", applicationId, podName, containerName);
        modelRouterMetricService.start(applicationProperties.getMetrics().getMetricsPollInterval(),
            applicationProperties.getMetrics().getMetricsWaitTimeout(),
            applicationId, podName, containerName);
        return modelRouterMetricService.registerStatusEmitter(applicationId, podName, containerName);
    }

    @DeleteMapping("/model-router-metrics/{applicationId}")
    public void stopModelRouterMetrics(
        @PathVariable(name = "applicationId") String applicationId,
        @RequestParam(name = "podName") String podName,
        @RequestParam(name = "containerName") String containerName) {
        log.debug("REST request to stop model router metrics for application {} and container {}", applicationId, containerName);
        modelRouterMetricService.stop(applicationId, podName, containerName);
    }

    @GetMapping(value = "/model-worker-metrics/{applicationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startModelWorkerMetrics(
        @PathVariable(name = "applicationId") String applicationId,
        @RequestParam(name = "podName") String podName,
        @RequestParam(name = "containerName") String containerName) {
        log.debug("REST request to start model worker metrics for application {} and container {}", applicationId, containerName);
        modelWorkerMetricService.start(applicationProperties.getMetrics().getMetricsPollInterval(),
            applicationProperties.getMetrics().getMetricsWaitTimeout(), applicationId, podName, containerName);
        return modelWorkerMetricService.registerStatusEmitter(applicationId, podName, containerName);
    }

    @DeleteMapping("/model-worker-metrics/{applicationId}")
    public void stopModelWorkerMetrics(
        @PathVariable(name = "applicationId") String applicationId,
        @RequestParam(name = "podName") String podName,
        @RequestParam(name = "containerName") String containerName) {
        log.debug("REST request to stop model worker metrics for application {} and container {}", applicationId, containerName);
        modelWorkerMetricService.stop(applicationId, podName, containerName);
    }

    @GetMapping(value = "/ray-job-metrics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startRayJobMetrics(@RequestParam("ids") String ids) {
        log.trace("REST request to start metrics for ray jobs {}", ids);
        var jobIds = ids.split(",");
        rayJobMetricService.start(applicationProperties.getMetrics().getMetricsPollInterval(),
            applicationProperties.getMetrics().getMetricsWaitTimeout(), jobIds);
        return rayJobMetricService.registerStatusEmitter(jobIds);
    }

    @DeleteMapping("/ray-job-metrics")
    public void stopRayJobMetrics(
        @RequestParam("ids") String ids) {
        var jobIds = ids.split(",");
        rayJobMetricService.stop(jobIds);
    }
}

