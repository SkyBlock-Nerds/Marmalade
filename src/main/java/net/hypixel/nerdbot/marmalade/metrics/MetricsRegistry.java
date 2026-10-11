package net.hypixel.nerdbot.marmalade.metrics;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Factory and lifecycle utilities for Prometheus metrics, wrapping the Prometheus Java client to
 * reduce boilerplate when registering counters, gauges, histograms, and summaries.
 */
@UtilityClass
public class MetricsRegistry {

    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    private static final NamespacedMetrics UNPREFIXED = new NamespacedMetrics(null);

    /**
     * Returns a factory whose metrics are all named {@code <namespace>_<name>}, so several bots or
     * services can share one Prometheus/Grafana setup without their metric names colliding.
     *
     * @param namespace the prefix, e.g. {@code "nerdbot"}
     * @throws IllegalArgumentException if the namespace isn't a valid Prometheus name prefix
     */
    public static NamespacedMetrics namespace(String namespace) {
        if (namespace == null || !VALID_NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid metrics namespace: '" + namespace + "'");
        }
        return new NamespacedMetrics(namespace);
    }

    /**
     * Builds and registers a Prometheus counter with optional label dimensions.
     *
     * @param name       the metric name
     * @param help       human-readable description of what the counter measures
     * @param labelNames zero or more label names to partition the counter
     * @return the registered {@link Counter}
     */
    public static Counter counter(String name, String help, String... labelNames) {
        return UNPREFIXED.counter(name, help, labelNames);
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
        return UNPREFIXED.gauge(name, help, labelNames);
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
        return UNPREFIXED.histogram(name, help, labelNames);
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
        return UNPREFIXED.histogram(name, help, buckets, labelNames);
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
        return UNPREFIXED.summary(name, help, labelNames);
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
