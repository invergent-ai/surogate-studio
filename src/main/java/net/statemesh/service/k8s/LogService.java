package net.statemesh.service.k8s;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.*;
import net.statemesh.service.dto.LogDTO;
import net.statemesh.service.dto.ResourceDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.statemesh.config.K8Timeouts.READ_LOGS_TIMEOUT_SECONDS;

@Service
@Slf4j
public class LogService extends EventStreamService {
    private Integer limit;
    private Integer sinceSeconds;
    private record ParseResult(LogDTO log, Instant lastTimestamp) {}

    public LogService(ApplicationService applicationService,
                      ContainerService containerService,
                      TaskRunService taskRunService,
                      RayJobService rayJobService,
                      KubernetesController kubernetesController,
                      ApplicationProperties applicationProperties,
                      @Qualifier("statusScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(applicationService, containerService, taskRunService, rayJobService, kubernetesController, applicationProperties, taskScheduler);
    }

    public void start(Integer limit, Integer sinceSeconds, Long pollInterval, Long pollTimeout, String... emId) {
        this.limit = limit;
        this.sinceSeconds = sinceSeconds;
        // poll a single time to stream logs
        super.start(pollInterval, pollTimeout, emId);
    }

    @Override
    @Retryable(retryFor = {TimeoutException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    protected void run(String... emId) throws TimeoutException, InterruptedException {
        var resourceType = emId[0];
        var resourceId = emId[1];
        var podName = emId[2];
        var containerId = emId[3];

        try {
            ResourceDTO resource = switch (resourceType) {
                case "application" -> applicationService.findOne(resourceId).orElseThrow(
                    () -> new RuntimeException("Application " + resourceId + " not found")
                );
                case "taskRun" -> taskRunService.findOne(resourceId).orElseThrow(
                    () -> new RuntimeException("TaskRun " + resourceId + " not found")
                );
                case "rayJob" -> rayJobService.findOne(resourceId).orElseThrow(
                    () -> new RuntimeException("RayJob " + resourceId + " not found")
                );
                default -> throw new RuntimeException("Unknown resource type " + resourceType);
            };

            TaskResult<InputStream> result = this.kubernetesController.readLogs(
                resource.getDeployedNamespace(),
                resource.getProject().getCluster(),
                null, limit, sinceSeconds, podName, null, true
            ).get(READ_LOGS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.isSuccess() || result.getValue() == null) {
                stop(emId);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(result.getValue(), StandardCharsets.UTF_8))) {
                String line;
                Instant lastTs = Instant.EPOCH;
                while ((line = reader.readLine()) != null) {
                    if (StringUtils.isNotBlank(line)) {
                        ParseResult pr = parseLogLine(line, lastTs);
                        lastTs = pr.lastTimestamp();
                        sendLogUpdate(Collections.singletonList(pr.log()), emId);
                    }
                }
            } catch (IOException e) {
                log.info("Stopped reading log stream for endpoint {}: {}", endpoint(), e.getMessage());
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendLogUpdate(List<LogDTO> logs, String... emId) {
        try {
            sendEvent("logs", logs, emId);
        } catch (Exception e) {
            log.error("Failed to send logs to SSE for resource {}", emId, e);
        }
    }

    private ParseResult parseLogLine(String line, Instant fallbackTimestamp) {
        LogDTO result = new LogDTO();
        final String[] parts = line.split("\\s+");
        if (parts.length > 0) {
            try {
                LocalDateTime parsed = LocalDateTime.parse(parts[0],
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'"));
                Instant ts = parsed.toInstant(ZoneOffset.UTC);
                result.setTimestamp(ts);
                result.setMessage(line.substring(parts[0].length()));
                return new ParseResult(result, ts);
            } catch (Exception e) {
                result.setTimestamp(fallbackTimestamp);
                result.setMessage(line);
                return new ParseResult(result, fallbackTimestamp);
            }
        } else {
            result.setTimestamp(fallbackTimestamp);
            result.setMessage(line);
            return new ParseResult(result, fallbackTimestamp);
        }
    }


    public List<LogDTO> fetchHistory(String resourceType, String resourceId, String podName,
                                     String containerId, Integer limit, Integer sinceSeconds,
                                     Integer tailLines)
        throws TimeoutException, InterruptedException {

        ResourceDTO resource = switch (resourceType) {
            case "application" -> applicationService.findOne(resourceId).orElseThrow(
                () -> new RuntimeException("Application " + resourceId + " not found")
            );
            case "taskRun" -> taskRunService.findOne(resourceId).orElseThrow(
                () -> new RuntimeException("TaskRun " + resourceId + " not found")
            );
            case "rayJob" -> rayJobService.findOne(resourceId).orElseThrow(
                () -> new RuntimeException("RayJob " + resourceId + " not found")
            );
            default -> throw new RuntimeException("Unknown resource type " + resourceType);
        };

        try {
            TaskResult<InputStream> result = this.kubernetesController.readLogs(
                resource.getDeployedNamespace(),
                resource.getProject().getCluster(),
                null, tailLines, sinceSeconds, podName, containerId, false
            ).get(READ_LOGS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.isSuccess() || result.getValue() == null) {
                return Collections.emptyList();
            }

            List<LogDTO> allLogs = new ArrayList<>();
            Instant lastTs = Instant.EPOCH;
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(result.getValue(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (StringUtils.isNotBlank(line)) {
                        ParseResult pr = parseLogLine(line, lastTs);
                        lastTs = pr.lastTimestamp();
                        allLogs.add(pr.log());
                    }
                }
            }

            // Return only the first `limit` entries (oldest in this batch)
            if (limit != null && allLogs.size() > limit) {
                return allLogs.subList(0, limit);
            }
            return allLogs;

        } catch (ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
