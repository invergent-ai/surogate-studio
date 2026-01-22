package net.statemesh.service.k8s;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.task.control.ControlTask;
import net.statemesh.k8s.util.FastPipedOutputStream;
import net.statemesh.service.ApplicationService;
import net.statemesh.service.ContainerService;
import net.statemesh.service.RayJobService;
import net.statemesh.service.TaskRunService;
import net.statemesh.service.dto.LineDTO;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
@Slf4j
public class TerminalService extends ResourceContext {
    protected static final Integer TERMINAL_COLUMNS = 100;
    protected static final Integer TERMINAL_PIPE_BUFFER = 4096;

    private final SimpMessagingTemplate messagingTemplate;
    private final AsyncTaskExecutor smTaskExecutor;

    @Builder
    record Terminal(List<Future<?>> futures,
                    PipedInputStream inputStream,
                    BufferedWriter inputOutWriter,
                    FastPipedOutputStream outputStream,
                    BufferedInputStream outputInStream,
                    BlockingQueue<LineDTO> commands) {
        public Terminal withFutures(List<Future<?>> futures) {
            return new Terminal(futures, inputStream(), inputOutWriter(), outputStream(), outputInStream(), commands);
        }
    }

    private final Map<String, Terminal> terminals = new ConcurrentHashMap<>();

    public TerminalService(ApplicationService applicationService,
                           ContainerService containerService,
                           TaskRunService taskRunService,
                           RayJobService rayJobService,
                           SimpMessagingTemplate messagingTemplate,
                           KubernetesController kubernetesController,
                           ApplicationProperties applicationProperties,
                           AsyncTaskExecutor smTaskExecutor) {
        super(applicationService, containerService, taskRunService, rayJobService,
            kubernetesController, applicationProperties);
        this.messagingTemplate = messagingTemplate;
        this.smTaskExecutor = smTaskExecutor;
    }

    public void startTerminal(ControlTask.ControlObject objectType, String... termId) {
        var termKey = key(termId);
        var termEndpoint = endpoint(termId);

        log.info("Starting terminal for {}", key(termId));
        stopTerminal(termId);
        Context context = buildContext(objectType, termId);

        try {
            PipedInputStream inputStream = new PipedInputStream();
            PipedInputStream outputInStream = new PipedInputStream(TERMINAL_PIPE_BUFFER);
            this.terminals.put(key(termId),
                Terminal.builder()
                    .inputStream(inputStream)
                    .outputInStream(new BufferedInputStream(outputInStream))
                    .outputStream(new FastPipedOutputStream(outputInStream))
                    .inputOutWriter(
                        new BufferedWriter(new OutputStreamWriter(new FastPipedOutputStream(inputStream)))
                    )
                    .commands(new LinkedBlockingQueue<>())
                    .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Future<?> readFuture = smTaskExecutor.submit(() -> readTerminalOutput(termId));
        Future<?> writeFuture = smTaskExecutor.submit(() -> sendTerminalCommand(termId));
        Future<?> future = smTaskExecutor.submit(() -> {
            TaskResult<Void> result;
            try {
                result = this.kubernetesController.getAppShell(
                    context.application().getDeployedNamespace(),
                    context.application(),
                    context.podName(),
                    context.container(),
                    context.application().getProject().getCluster(),
                    this.terminals.get(termKey).outputStream(),
                    this.terminals.get(termKey).inputStream(),
                    TERMINAL_COLUMNS
                ).get(applicationProperties.getMetrics().getShellWatchTimeout(), TimeUnit.SECONDS);

                if (!result.isSuccess() && !Thread.currentThread().isInterrupted()) {
                    log.error("Failed to start terminal in Kubernetes for {}", termKey);
                    messagingTemplate.convertAndSend("/topic/terminal/" + termEndpoint,
                        Map.of("type", "disconnect"));
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException | TimeoutException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    messagingTemplate.convertAndSend("/topic/terminal/" + termEndpoint,
                        Map.of("type", "timeout"));
                }
            }
        });

        this.terminals.put(termKey,
            this.terminals.get(termKey).withFutures(
                List.of(readFuture, writeFuture, future)
            ));
        log.info("Shell task started for {}", termKey);
    }

    public void stopTerminal(String... termId) {
        Terminal terminal = terminals.remove(key(termId));
        if (terminal != null) {
            log.debug("Stopping terminal for {}", key(termId));
            try {
                terminal.inputOutWriter().close();
                terminal.inputStream().close();
                terminal.outputInStream().close();
                terminal.outputStream().close();
            } catch (IOException e) {
                log.error("Error closing terminal", e);
            }
            terminal.futures().forEach(future -> future.cancel(true));
        }
    }

    private void sendTerminalOutput(byte[] terminalOutput, String... termId) {
        log.trace("Sending terminal output of size {} to /topic/terminal/{}", terminalOutput.length, endpoint(termId));
        try {
            messagingTemplate.convertAndSend("/topic/terminal/" + endpoint(termId), new TextMessage(terminalOutput));
            log.trace("Successfully sent terminal output to websocket for {}", key(termId));
        } catch (Exception e) {
            log.error("Failed to send terminal output to websocket for application {}", key(termId), e);
        }
    }

    public void enqueueTerminalCommand(LineDTO commandLine) {
        var key = keyFromCommandLine(commandLine);
        log.trace("Received terminal command {} for {}", commandLine.getMessage(), key);
        if (!this.terminals.containsKey(key)) {
            log.error("No input stream for terminal {}", key);
            return;
        }

        try {
            this.terminals
                .get(key)
                .commands()
                .put(commandLine);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendTerminalCommand(String... termId) {
        var queue = this.terminals.get(key(termId)).commands();
        LineDTO commandLine;
        var key = "";
        try {
            while ((commandLine = queue.poll(applicationProperties.getMetrics().getShellWatchTimeout(), TimeUnit.SECONDS)) != null) {
                key = keyFromCommandLine(commandLine);
                this.terminals.get(key)
                    .inputOutWriter().write(commandLine.getMessage());
                this.terminals.get(key)
                    .inputOutWriter().flush();
            }
        } catch (IOException e) {
            log.error("Error writing terminal command", e);
            messagingTemplate.convertAndSend(
                "/topic/terminal/" + endpoint(key),
                Map.of("type", "timeout")
            );
        } catch (InterruptedException e) {
            // Ignore - this was on purpose
        }
    }

    private void readTerminalOutput(String... termId) {
        int len;
        byte[] data = new byte[TERMINAL_PIPE_BUFFER];
        try {
            try (BufferedInputStream input = this.terminals.get(key(termId)).outputInStream()) {
                while (!Thread.currentThread().isInterrupted() && (len = input.read(data)) != -1) {
                    sendTerminalOutput(Arrays.copyOfRange(data, 0, len), termId);
                }
            }
        } catch (IOException e) {
            log.error("Error reading terminal output stream for {}", key(termId), e);
            messagingTemplate.convertAndSend("/topic/terminal/" + endpoint(termId),
                Map.of(
                    "type", "disconnect",
                    "error", "Stream interrupted: " + e.getMessage()
                )
            );
        }
    }

    private String keyFromCommandLine(LineDTO commandLine) {
        if (commandLine.getApplicationId() != null && commandLine.getPodName() != null && commandLine.getContainerId() != null) {
            return key(commandLine.getApplicationId(), commandLine.getPodName(), commandLine.getContainerId());
        } else if (commandLine.getVmId() != null) {
            return key(commandLine.getVmId());
        } else {
            throw new IllegalArgumentException("Invalid command line: " + commandLine);
        }
    }
}
