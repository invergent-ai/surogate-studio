package net.statemesh.k8s.util;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Async retry helper for CompletableFuture chains.
 * - Executes the supplied asynchronous computation.
 * - If it completes exceptionally and the retry predicate matches, schedules another attempt after the delay.
 * - Propagates the final result or the last exception (unwrapped) when attempts are exhausted.
 */
public final class AsyncRetry {
    private AsyncRetry() {}

    public static <T> CompletableFuture<T> retryAsync(Supplier<CompletableFuture<T>> supplier,
                                                      int maxAttempts,
                                                      Duration delay,
                                                      ThreadPoolTaskScheduler scheduler,
                                                      Predicate<Throwable> retryPredicate) {
        Objects.requireNonNull(supplier, "supplier");
        Objects.requireNonNull(delay, "delay");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(retryPredicate, "retryPredicate");
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        CompletableFuture<T> promise = new CompletableFuture<>();
        attempt(supplier, 1, maxAttempts, delay, scheduler, retryPredicate, promise);
        return promise;
    }

    public static <T> CompletableFuture<T> retryAsync(Supplier<CompletableFuture<T>> supplier,
                                                      int maxAttempts,
                                                      Duration delay,
                                                      ThreadPoolTaskScheduler scheduler) {
        return retryAsync(supplier, maxAttempts, delay, scheduler, t -> true);
    }

    public static <T> CompletableFuture<T> retryAsync(Supplier<CompletableFuture<T>> supplier,
                                                      int maxAttempts,
                                                      Duration delay,
                                                      ThreadPoolTaskScheduler scheduler,
                                                      Predicate<Throwable> retryPredicate,
                                                      BooleanSupplier cancelSignal) {
        Objects.requireNonNull(supplier, "supplier");
        Objects.requireNonNull(delay, "delay");
        Objects.requireNonNull(retryPredicate, "retryPredicate");
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        CompletableFuture<T> promise = new CompletableFuture<>();
        attemptCancellable(supplier, 1, maxAttempts, delay, scheduler, retryPredicate, promise, cancelSignal);
        return promise;
    }

    private static <T> void attempt(Supplier<CompletableFuture<T>> supplier,
                                    int attempt,
                                    int maxAttempts,
                                    Duration delay,
                                    ThreadPoolTaskScheduler scheduler,
                                    Predicate<Throwable> predicate,
                                    CompletableFuture<T> promise) {
        if (promise.isCancelled()) return;
        CompletableFuture<T> cf;
        try {
            cf = supplier.get();
        } catch (Throwable startup) {
            handleFailure(startup, supplier, attempt, maxAttempts, delay, scheduler, predicate, promise);
            return;
        }
        cf.whenComplete((value, error) -> {
            if (error == null) {
                promise.complete(value);
            } else {
                handleFailure(error, supplier, attempt, maxAttempts, delay, scheduler, predicate, promise);
            }
        });
    }

    private static <T> void attemptCancellable(Supplier<CompletableFuture<T>> supplier,
                                               int attempt,
                                               int maxAttempts,
                                               Duration delay,
                                               ThreadPoolTaskScheduler scheduler,
                                               Predicate<Throwable> predicate,
                                               CompletableFuture<T> promise,
                                               BooleanSupplier cancelSignal) {
        if (promise.isCancelled()) return;
        if (cancelSignal != null && cancelSignal.getAsBoolean()) {
            promise.completeExceptionally(new CancellationException("Retry cancelled"));
            return;
        }
        CompletableFuture<T> cf;
        try {
            cf = supplier.get();
        } catch (Throwable startup) {
            handleFailureCancellable(startup, supplier, attempt, maxAttempts, delay, scheduler, predicate, promise, cancelSignal);
            return;
        }
        cf.whenComplete((value, error) -> {
            if (error == null) {
                promise.complete(value);
            } else {
                handleFailureCancellable(error, supplier, attempt, maxAttempts, delay, scheduler, predicate, promise, cancelSignal);
            }
        });
    }

    private static <T> void handleFailure(Throwable error,
                                          Supplier<CompletableFuture<T>> supplier,
                                          int attempt,
                                          int maxAttempts,
                                          Duration delay,
                                          ThreadPoolTaskScheduler scheduler,
                                          Predicate<Throwable> predicate,
                                          CompletableFuture<T> promise) {
        Throwable cause = unwrap(error);
        if (attempt < maxAttempts && predicate.test(cause) && !promise.isCancelled()) {
            int nextAttempt = attempt + 1;
            scheduler.schedule(() -> attempt(supplier, nextAttempt, maxAttempts, delay, scheduler, predicate, promise),
                Instant.now().plusMillis(delay.toMillis()));
        } else {
            promise.completeExceptionally(cause);
        }
    }

    private static <T> void handleFailureCancellable(Throwable error,
                                                     Supplier<CompletableFuture<T>> supplier,
                                                     int attempt,
                                                     int maxAttempts,
                                                     Duration delay,
                                                     ThreadPoolTaskScheduler scheduler,
                                                     Predicate<Throwable> predicate,
                                                     CompletableFuture<T> promise,
                                                     BooleanSupplier cancelSignal) {
        Throwable cause = unwrap(error);
        if (attempt < maxAttempts && predicate.test(cause) && !promise.isCancelled() && (cancelSignal == null || !cancelSignal.getAsBoolean())) {
            int nextAttempt = attempt + 1;
            scheduler.schedule(() -> attemptCancellable(supplier, nextAttempt, maxAttempts, delay, scheduler, predicate, promise, cancelSignal),
                Instant.now().plusMillis(delay.toMillis()));
        } else {
            if (cancelSignal != null && cancelSignal.getAsBoolean()) {
                promise.completeExceptionally(new CancellationException("Retry cancelled"));
            } else {
                promise.completeExceptionally(cause);
            }
        }
    }

    private static Throwable unwrap(Throwable t) {
        while (t instanceof CompletionException || t instanceof ExecutionException) {
            if (t.getCause() == null) break;
            t = t.getCause();
        }
        return t;
    }
}
