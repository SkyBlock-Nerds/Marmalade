package net.hypixel.nerdbot.marmalade.metrics;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleCollector;
import io.prometheus.client.Summary;
import org.jetbrains.annotations.Nullable;

/**
 * Registers metrics with every name prefixed by a namespace, e.g. {@code nerdbot_events_total}.
 * Get one from {@link MetricsRegistry#namespace(String)}.
 */
public final class NamespacedMetrics {

    @Nullable
    private final String namespace;

    NamespacedMetrics(@Nullable String namespace) {
        this.namespace = namespace;
    }

    public Counter counter(String name, String help, String... labelNames) {
        return register(Counter.build(), name, help, labelNames);
    }

    public Gauge gauge(String name, String help, String... labelNames) {
        return register(Gauge.build(), name, help, labelNames);
    }

    public Histogram histogram(String name, String help, String... labelNames) {
        return register(Histogram.build(), name, help, labelNames);
    }

    public Histogram histogram(String name, String help, double[] buckets, String... labelNames) {
        return register(Histogram.build().buckets(buckets), name, help, labelNames);
    }

    public Summary summary(String name, String help, String... labelNames) {
        return register(Summary.build(), name, help, labelNames);
    }

    private <B extends SimpleCollector.Builder<B, C>, C extends SimpleCollector<?>> C register(B builder, String name, String help, String[] labelNames) {
        builder.name(name).help(help);
        if (namespace != null) {
            builder.namespace(namespace);
        }
        if (labelNames.length > 0) {
            builder.labelNames(labelNames);
        }
        return builder.register();
    }
}
