package net.statemesh.thread;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class VirtualWait {
    /**
     * Poll tries a condition func until it returns true, an exception, or the timeout is reached.
     *
     * @param interval the check interval
     * @param timeout the timeout period
     * @param condition the condition func
     */
    public static boolean poll(Duration interval, Duration timeout, Supplier<Boolean> condition) {
        return poll(interval, interval, timeout, condition);
    }

    /**
     * Poll tries a condition func until w/ the initial delay specified.
     *
     * @param initialDelay the initial delay
     * @param interval the check interval
     * @param timeout the timeout period
     * @param condition the condition
     * @return returns true if gracefully finished
     */
    public static boolean poll(
        Duration initialDelay, Duration interval, Duration timeout, Supplier<Boolean> condition) {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual().factory();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(virtualThreadFactory);
        AtomicBoolean result = new AtomicBoolean(false);
        long dueDate = System.currentTimeMillis() + timeout.toMillis();
        ScheduledFuture<?> future =
            executorService.scheduleAtFixedRate(
                () -> {
                    try {
                        result.set(condition.get());
                    } catch (Exception e) {
                        result.set(false);
                    }
                },
                initialDelay.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS);
        try {
            while (true) {
                if (System.currentTimeMillis() >= dueDate) {
                    break;
                } else {
                    if (result.get()) {
                        future.cancel(true);
                        return true;
                    }

                    Thread.sleep(interval.toMillis() / 10);
                }
            }
        } catch (Exception e) {
            return result.get();
        }
        future.cancel(true);
        return result.get();
    }

    public static Object pollWithResult(
        Duration initialDelay, Duration interval, Duration timeout, Supplier<Object> condition) {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual().factory();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(virtualThreadFactory);
        AtomicReference<Object> result = new AtomicReference<>();
        long dueDate = System.currentTimeMillis() + timeout.toMillis();
        ScheduledFuture<?> future =
            executorService.scheduleAtFixedRate(
                () -> {
                    try {
                        result.set(condition.get());
                    } catch (Exception e) {
                        result.set(e);
                    }
                },
                initialDelay.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS);
        try {
            while (true) {
                if (System.currentTimeMillis() >= dueDate) {
                    break;
                } else {
                    if (result.get() != null) {
                        future.cancel(true);
                        return result.get();
                    }

                    Thread.sleep(interval.toMillis() / 10);
                }
            }
        } catch (Exception e) {
            return result.get();
        }
        future.cancel(true);
        return result.get();
    }
}
