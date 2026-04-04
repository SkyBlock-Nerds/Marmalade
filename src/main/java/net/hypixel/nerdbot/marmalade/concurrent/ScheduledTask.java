package net.hypixel.nerdbot.marmalade.concurrent;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A named, lifecycle-managed repeating task that runs a {@link Runnable} at a fixed rate on a
 * dedicated daemon thread, with graceful start and stop semantics.
 */
@Slf4j
public class ScheduledTask implements AutoCloseable {

    @Getter
    private final String name;

    private final Runnable task;
    private final Duration initialDelay;
    private final Duration interval;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    private ScheduledTask(String name, Runnable task, Duration initialDelay, Duration interval) {
        this.name = name;
        this.task = task;
        this.initialDelay = initialDelay;
        this.interval = interval;
    }

    /**
     * Creates a new {@code ScheduledTask} that starts executing immediately (no initial delay) at the given interval.
     *
     * @param name the display name of the task, also used as the backing thread name
     * @param task the runnable to execute on each tick
     * @param interval the fixed period between successive executions
     * @return a new, not-yet-started {@code ScheduledTask}
     */
    public static ScheduledTask create(String name, Runnable task, Duration interval) {
        return create(name, task, Duration.ZERO, interval);
    }

    /**
     * Creates a new {@code ScheduledTask} that waits for an initial delay before its first execution,
     * then repeats at the given interval.
     *
     * @param name the display name of the task, also used as the backing thread name
     * @param task the runnable to execute on each tick
     * @param initialDelay the delay before the first execution
     * @param interval the fixed period between successive executions
     * @return a new, not-yet-started {@code ScheduledTask}
     */
    public static ScheduledTask create(String name, Runnable task, Duration initialDelay, Duration interval) {
        return new ScheduledTask(name, task, initialDelay, interval);
    }

    /**
     * Starts the scheduled task on a new daemon thread, scheduling it at the configured initial delay and interval.
     * If the task is already running, a warning is logged and this call is a no-op.
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("ScheduledTask '{}' is already running; ignoring start()", name);
            return;
        }

        executor = ExecutorFactory.newScheduledDaemon(name);

        Runnable wrappedTask = () -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Uncaught exception in ScheduledTask '{}'", name, e);
            }
        };

        future = executor.scheduleAtFixedRate(
            wrappedTask,
            initialDelay.toMillis(),
            interval.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stops the scheduled task, waiting up to 5 seconds for the executor to terminate gracefully.
     * If the task is not running, this call is a no-op.
     */
    public void stop() {
        stop(Duration.ofSeconds(5));
    }

    /**
     * Stops the scheduled task, waiting up to the given timeout for the executor to terminate gracefully
     * before forcing a shutdown. If the task is not running, this call is a no-op.
     *
     * @param timeout the maximum time to wait for graceful termination before calling {@code shutdownNow()}
     */
    public void stop(Duration timeout) {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        if (future != null) {
            future.cancel(false);
        }

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    log.warn("ScheduledTask '{}' did not terminate within {}ms; forcing shutdown", name, timeout.toMillis());
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }

    /**
     * Returns whether this task is currently scheduled and running.
     *
     * @return {@code true} if the task has been started and not yet stopped, {@code false} otherwise
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Stops the task, implementing {@link AutoCloseable} for use in try-with-resources blocks.
     * Delegates to {@link #stop()}.
     */
    @Override
    public void close() {
        stop();
    }
}
