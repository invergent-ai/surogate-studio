package net.statemesh.node;

import net.statemesh.thread.VirtualWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static net.statemesh.node.TaskStatus.ExitCodeUnknown;

public class NodeTask implements Callable<CompletableFuture<TaskStatus>> {
    private final Logger log = LoggerFactory.getLogger(NodeTask.class);

    private static final int HTTP_CONTROL_PORT = 6201;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String nodeIp;
    private final TaskBody task;
    private final String authToken;
    private final boolean shouldPoll;

    public NodeTask(String nodeIp, TaskBody task, String authToken, boolean shouldPoll) {
        this.nodeIp = nodeIp;
        this.task = task;
        this.authToken = authToken;
        this.shouldPoll = shouldPoll;
    }

    @Override
    public CompletableFuture<TaskStatus> call() {
        var submitUrl = String.format("http://%s:%d/tasks", nodeIp, HTTP_CONTROL_PORT);
        var statusUrl = String.format("http://%s:%d/status?id={id}", nodeIp, HTTP_CONTROL_PORT);
        HttpHeaders headers = new HttpHeaders();
        headers.set("XSM", authToken);
        var requestEntity = new HttpEntity<>(task, headers);
        var response = restTemplate.exchange(submitUrl, HttpMethod.POST, requestEntity, Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to submit task to node {}: {}", nodeIp, task);
            return CompletableFuture.failedFuture(new NodeTaskSubmitError(nodeIp, task.id));

        }

        log.info("Task submitted to node {}: {}", nodeIp, task);

        if (!shouldPoll)
            return CompletableFuture.completedFuture(null);

        var result = VirtualWait.pollWithResult(
            Duration.ofSeconds(2),
            Duration.ofSeconds(5),
            Duration.ofSeconds(task.timeout == 0 ? 1_000_000_000 : 5 * task.timeout),
            () -> {
                var requestEntity2 = new HttpEntity<>(null, headers);
                var statusResponse = restTemplate.exchange(statusUrl, HttpMethod.GET, requestEntity2, TaskStatus.class, Map.of("id", task.id));
                if (!statusResponse.getStatusCode().is2xxSuccessful()) {
                    log.error("Failed to get task status from node {}: {}", nodeIp, task);
                    return new NodeTaskSubmitError(nodeIp, task.id);
                }

                var body = statusResponse.getBody();
                if (body == null) {
                    return null;
                }

                if (body.getExitCode() == ExitCodeUnknown) {
                    // keep polling if we don't have a final status
                    return null;
                } else {
                    return body;
                }
            }
        );

        if (result == null) {
            // timeout, should not happen
            return CompletableFuture.failedFuture(new NodeTaskTimeoutError(nodeIp, task.id));
        } else if (result instanceof Throwable) {
            return CompletableFuture.failedFuture((Throwable) result);
        } else {
            return CompletableFuture.completedFuture((TaskStatus) result);
        }
    }
}
