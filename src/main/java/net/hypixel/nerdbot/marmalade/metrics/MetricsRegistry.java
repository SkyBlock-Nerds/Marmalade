package net.hypixel.nerdbot.marmalade.metrics;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import lombok.experimental.UtilityClass;

import java.io.IOException;

/**
 * Factory and lifecycle utilities for Prometheus metrics, wrapping the Prometheus Java client to
 * reduce boilerplate when registering counters, gauges, histograms, and summaries.
 */
@UtilityClass
public class MetricsRegistry {

    /**
     * Builds and registers a Prometheus counter with optional label dimensions.
     *
     * @param name       the metric name
     * @param help       human-readable description of what the counter measures
     * @param labelNames zero or more label names to partition the counter
     * @return the registered {@link Counter}
     */
    public static Counter counter(String name, String help, String... labelNames) {
        Counter.Builder builder = Counter.build().name(name).help(help);
        if (labelNames.length > 0) {
            builder.labelNames(labelNames);
        }
        return builder.register();
    }

    /**
     * Builds and registers a Prometheus gauge with optional label dimensions.
     *
     * @param name       the metric name
     * @param help       human-readable description of what the gauge measures
     * @param labelNames zero or more label names to partition the gauge
     * @return the registered {@link Gauge}
     */
    public static Gauge gauge(String name, String help, String... labelNames) {
        Gauge.Builder builder = Gauge.build().name(name).help(help);
        if (labelNames.length > 0) {
            builder.labelNames(labelNames);
        }
        return builder.register();
    }

    /**
     * Builds and registers a Prometheus histogram with default buckets and optional label dimensions.
     *
     * @param name       the metric name
     * @param help       human-readable description of what the histogram measures
     * @param labelNames zero or more label names to partition the histogram
     * @return the registered {@link Histogram}
     */
    public static Histogram histogram(String name, String help, String... labelNames) {
        Histogram.Builder builder = Histogram.build().name(name).help(help);
        if (labelNames.length > 0) {
            builder.labelNames(labelNames);
        }
        return builder.register();
    }

    /**
     * Builds and registers a Prometheus histogram with explicit bucket boundaries and optional label dimensions.
     *
     * @param name       the metric name
     * @param help       human-readable description of what the histogram measures
     * @param buckets    explicit upper-bound values defining each histogram bucket
     * @param labelNames zero or more label names to partition the histogram
     * @return the registered {@link Histogram}
     */
    public static Histogram histogram(String name, String help, double[] buckets, String... labelNames) {
        Histogram.Builder builder = Histogram.build().name(name).help(help).buckets(buckets);
        if (labelNames.length > 0) {
            builder.labelNames(labelNames);
        }
        return builder.register();
    }

    /**
     * Builds and registers a Prometheus summary with optional label dimensions.
     *
     * @param name       the metric name
     * @param help       human-readable description of what the summary measures
     * @param labelNames zero or more label names to partition the summary
     * @return the registered {@link Summary}
     */
    public static Summary summary(String name, String help, String... labelNames) {
        Summary.Builder builder = Summary.build().name(name).help(help);
        if (labelNames.length > 0) {
            builder.labelNames(labelNames);
        }
        return builder.register();
    }

    /**
     * Starts a Prometheus HTTP scrape endpoint on the given port, registering default JVM exports
     * before binding.
     *
     * @param port the TCP port to listen on
     * @return the running {@link HTTPServer}
     * @throws IOException if the server cannot bind to the specified port
     */
    public static HTTPServer startServer(int port) throws IOException {
        return startServer(port, true);
    }

    /**
     * Starts a Prometheus HTTP scrape endpoint on the given port, optionally registering default
     * JVM exports (memory, GC, thread metrics) before binding.
     *
     * @param port                  the TCP port to listen on
     * @param includeDefaultExports if {@code true}, {@link #registerDefaults()} is called before starting
     * @return the running {@link HTTPServer}
     * @throws IOException if the server cannot bind to the specified port
     */
    public static HTTPServer startServer(int port, boolean includeDefaultExports) throws IOException {
        if (includeDefaultExports) {
            registerDefaults();
        }
        return new HTTPServer.Builder().withPort(port).build();
    }

    /**
     * Registers the default Prometheus JVM collectors (memory pools, GC stats, thread counts, etc.)
     * with the global registry. Safe to call multiple times - subsequent calls are no-ops.
     */
    public static void registerDefaults() {
        DefaultExports.initialize();
    }
}
