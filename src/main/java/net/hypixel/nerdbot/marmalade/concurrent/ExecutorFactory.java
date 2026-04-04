package net.hypixel.nerdbot.marmalade.concurrent;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility for creating daemon-backed {@link java.util.concurrent.ExecutorService} and
 * {@link ScheduledExecutorService} instances with consistently named threads.
 */
@UtilityClass
public class ExecutorFactory {

    /**
     * Creates a single-threaded scheduled executor backed by a daemon thread with the given name.
     *
     * @param name the name assigned to the daemon thread
     * @return a new single-thread {@link ScheduledExecutorService}
     */
    public static ScheduledExecutorService newScheduledDaemon(String name) {
        return Executors.newSingleThreadScheduledExecutor(daemonThreadFactory(name));
    }

    /**
     * Creates a fixed-size scheduled thread pool backed by numbered daemon threads using the given prefix.
     *
     * @param prefix the prefix used to name each thread (e.g. "my-pool" produces "my-pool-1", "my-pool-2", ...)
     * @param poolSize the number of threads in the pool
     * @return a new {@link ScheduledExecutorService} with the specified pool size
     */
    public static ScheduledExecutorService newScheduledDaemonPool(String prefix, int poolSize) {
        return Executors.newScheduledThreadPool(poolSize, daemonThreadFactory(prefix, true));
    }

    /**
     * Creates a single-threaded executor backed by a daemon thread with the given name.
     *
     * @param name the name assigned to the daemon thread
     * @return a new single-thread {@link ExecutorService}
     */
    public static ExecutorService newDaemon(String name) {
        return Executors.newSingleThreadExecutor(daemonThreadFactory(name));
    }

    /**
     * Creates a fixed-size thread pool backed by numbered daemon threads using the given prefix.
     *
     * @param prefix the prefix used to name each thread (e.g. "my-pool" produces "my-pool-1", "my-pool-2", ...)
     * @param poolSize the number of threads in the pool
     * @return a new fixed-thread-pool {@link ExecutorService} with the specified pool size
     */
    public static ExecutorService newDaemonPool(String prefix, int poolSize) {
        return Executors.newFixedThreadPool(poolSize, daemonThreadFactory(prefix, true));
    }

    /**
     * Creates a {@link ThreadFactory} that produces a single named daemon thread.
     *
     * @param name the fixed name assigned to every thread created by the factory
     * @return a daemon {@link ThreadFactory} using the given name
     */
    public static ThreadFactory daemonThreadFactory(String name) {
        return daemonThreadFactory(name, false);
    }

    /**
     * Creates a {@link ThreadFactory} that produces daemon threads, optionally with an incrementing
     * numeric suffix appended to the given prefix.
     *
     * @param prefix the base name (or full name if {@code numbered} is {@code false}) for created threads
     * @param numbered if {@code true}, each thread is named "{@code prefix-N}" with an auto-incrementing counter
     * @return a daemon {@link ThreadFactory} using the given naming scheme
     */
    public static ThreadFactory daemonThreadFactory(String prefix, boolean numbered) {
        AtomicInteger counter = new AtomicInteger(0);
        return runnable -> {
            String threadName = numbered ? prefix + "-" + counter.incrementAndGet() : prefix;
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        };
    }
}
