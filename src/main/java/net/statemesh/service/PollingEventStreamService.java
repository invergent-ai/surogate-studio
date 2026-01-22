package net.statemesh.service;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.service.k8s.ResourceContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class PollingEventStreamService extends ResourceContext {
    protected final Map<String, PollHandle> statuses = new ConcurrentHashMap<>();
    protected final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    protected Long pollInterval;
    protected Long pollTimeout;
    private final ThreadPoolTaskScheduler taskScheduler;

    public PollingEventStreamService(
        ApplicationService applicationService,
        ContainerService containerService,
        TaskRunService taskRunService,
        RayJobService rayJobService,
        KubernetesController kubernetesController,
        ApplicationProperties applicationProperties,
        ThreadPoolTaskScheduler taskScheduler) {
        super(applicationService, containerService, taskRunService, rayJobService, kubernetesController, applicationProperties);
        this.taskScheduler = taskScheduler;
    }

    public PollHandle start(Long pollInterval, Long pollTimeout, String... emId) {
        final var emKey = key(emId);
        this.pollInterval = pollInterval;
        this.pollTimeout = pollTimeout;

        log.debug("Starting poll task for key {} with interval {}s and timeout {}s", key(emId), pollInterval, pollTimeout);
        return statuses.computeIfAbsent(emKey, key -> {
            PollHandle handle = new PollHandle();
            long intervalMs = pollInterval * 1000L;
            AtomicBoolean firstRun = new AtomicBoolean(true);

            handle.future = taskScheduler.scheduleAtFixedRate(() -> {
                if (handle.cancelled) {
                    return; // graceful early exit
                }

                // Skip connection check on first run to allow emitter registration
                if (!firstRun.compareAndSet(true, false)) {
                    // Check if any emitters are still connected
                    var emitterList = emitters.get(key);
                    if (emitterList != null) {
                        boolean hasActiveConnections = false;
                        for (SseEmitter emitter : emitterList) {
                            if (!isConnectionOpen(emitter)) {
                                removeEmitter(emitter, emId);
                            } else {
                                hasActiveConnections = true;
                            }
                        }
                        // If all connections are closed, stop polling
                        if (!hasActiveConnections) {
                            log.debug("All SSE connections closed for key {}, stopping poll", key);
                            emitters.remove(key);
                            handle.cancelled = true;
                            handle.future.cancel(true);
                            statuses.remove(key);
                            return;
                        }
                    } else {
                        // No emitters registered, stop polling
                        log.debug("No SSE emitters registered for key {}, stopping poll", key);
                        handle.cancelled = true;
                        handle.future.cancel(true);
                        statuses.remove(key);
                        return;
                    }
                }

                if (!handle.inProgress.compareAndSet(false, true)) {
                    log.trace("Skipping overlapping poll for key {}", key);
                    return;
                }

                try {
                    onTick(emId);
                } catch (InterruptedException | TimeoutException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        // Per-call timeout (not the overall guard)
                        completeEmitters(true, "Polling timeout", emId);
                    }
                    Thread.currentThread().interrupt();
                }  catch (Exception e) {
                    log.error("Unhandled exception during status poll for key {}", key, e);
                } finally {
                    handle.inProgress.set(false);
                }
            }, Duration.ofMillis(intervalMs));

            // Global timeout guard – cancels the schedule and completes SSE emitters.
            taskScheduler.schedule(() -> {
                if (!handle.future.isCancelled()) {
                    log.debug("Canceling poll task {} due to timeout after {} seconds",
                        key,pollTimeout);
                    handle.cancelled = true;
                    handle.future.cancel(true);
                    statuses.remove(key);
                    completeEmitters(true, "Timeout reached", emId);
                }
            }, Instant.now().plusSeconds(pollTimeout));
            return handle;
        });
    }

    protected abstract void onTick(String... emId) throws InterruptedException, TimeoutException;

    public void stop(String... emId) {
        final var key = key(emId);
        PollHandle handle = statuses.remove(key);
        if (handle != null) {
            log.debug("Stopping poll for key {}", key);
            handle.cancelled = true;
            handle.future.cancel(true);
        }
        completeEmitters(false, "Stopped", emId);
    }

    protected void sendEvent(String name, Object data, String... emId) {
        final var key = key(emId);
        var emitterList = emitters.get(key);
        if (emitterList != null) {
            for (SseEmitter emitter : emitterList) {
                try {
                    emitter.send(SseEmitter.event().name(name).data(data));
                    log.trace("Successfully sent event to SSE for key {}", key);
                } catch (IOException e) {
                    log.debug("Removing SSE emitter for poll {} due to: {}", key, e.getCause().getClass().getSimpleName());
                    emitter.completeWithError(e);
                    removeEmitter(emitter, emId);
                }
            }
        }
    }

    public SseEmitter registerStatusEmitter(String... emId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(key(emId), k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(emitter, emId);
        });
        emitter.onCompletion(() -> removeEmitter(emitter, emId));
        emitter.onError(t -> {
            removeEmitter(emitter, emId);
        });
        return emitter;
    }

    protected void removeEmitter(SseEmitter emitter, String... emId) {
        final var key = key(emId);
        var list = emitters.get(key);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                // no more listeners, stop polling
                emitters.remove(key);
                stop(key);
            }
        }
    }

    protected void completeEmitters(boolean timeout, String msg, String... emId) {
        var list = emitters.remove(key(emId));
        if (list != null) {
            for (SseEmitter emitter : list) {
                try {
                    if (timeout) {
                        emitter.send(SseEmitter.event().name("timeout").data(msg));
                    } else if (msg != null) {
                        emitter.send(SseEmitter.event().name("complete").data(msg));
                    }
                } catch (Exception ignored) {
                } finally {
                    emitter.complete();
                }
            }
        }
    }

    protected boolean isConnectionOpen(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment(""));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static class PollHandle {
        public ScheduledFuture<?> future;
        public final AtomicBoolean inProgress = new AtomicBoolean(false);
        public volatile boolean cancelled = false;
    }
}
