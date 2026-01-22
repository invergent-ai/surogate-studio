package net.statemesh.k8s.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.ClusterDTO;

@Builder
@Getter
@AllArgsConstructor
public class TaskResult<T> {
    private boolean success;
    private CreationStatus creationStatus;
    private boolean waitTimeout;
    private T value;
    private ClusterDTO cluster;
    private ApplicationDTO applicationDTO;

    public enum CreationStatus {
        CREATED,
        SKIPPED_EXISTS
    }

    public TaskResult(boolean waitTimeout) {
        this.waitTimeout = waitTimeout;
    }

    public static <T> TaskResult<T> waitTimeout() {
        return new TaskResult<>(Boolean.TRUE);
    }

    public static <T> TaskResult<T> success() {
        return new TaskResult<>(Boolean.TRUE, null, Boolean.FALSE, null, null, null);
    }

    public static <T> TaskResult<T> fail() {
        return new TaskResult<>(Boolean.FALSE, null, Boolean.FALSE, null, null, null);
    }

    public static <T> TaskResult<T> from(TaskResult<?> other) {
        return new TaskResult<>(other.success, other.creationStatus, other.waitTimeout, null, other.cluster, other.applicationDTO);
    }

    @SafeVarargs
    public static <T> TaskResult<T> join(TaskResult<T>... results) {
        for (TaskResult<T> result : results) {
            // Return fail if any of the results is fail
            if (result.isFailed()) {
                return TaskResult.fail();
            }
            // Return waitTimeout if any of the results is waitTimeout
            if (result.isWaitTimeout()) {
                return TaskResult.waitTimeout();
            }
        }
        return TaskResult.success();
    }

    public TaskResult<T> value(T value) {
        this.value = value;
        return this;
    }

    public TaskResult<T> cluster(ClusterDTO cluster) {
        this.cluster = cluster;
        return this;
    }

    public boolean isFailed() {
        return !success && !waitTimeout;
    }
}
