package net.hypixel.nerdbot.marmalade.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NamespacedMetricsTest {

    private static final CollectorRegistry REGISTRY = CollectorRegistry.defaultRegistry;

    @AfterEach
    void cleanUp() {
        REGISTRY.clear();
    }

    @Test
    void counterNameIsPrefixed() {
        Counter counter = MetricsRegistry.namespace("nerdbot").counter("events_total", "Events");
        counter.inc();

        assertThat(REGISTRY.getSampleValue("nerdbot_events_total")).isEqualTo(1.0);
        assertThat(REGISTRY.getSampleValue("events_total")).isNull();
    }

    @Test
    void labelsStillWork() {
        Counter counter = MetricsRegistry.namespace("nerdbot").counter("commands_total", "Commands", "command");
        counter.labels("ping").inc();

        assertThat(REGISTRY.getSampleValue("nerdbot_commands_total", new String[]{"command"}, new String[]{"ping"})).isEqualTo(1.0);
    }

    @Test
    void gaugeNameIsPrefixed() {
        Gauge gauge = MetricsRegistry.namespace("nerdbot").gauge("guild_users", "Users");
        gauge.set(42);

        assertThat(REGISTRY.getSampleValue("nerdbot_guild_users")).isEqualTo(42.0);
    }

    @Test
    void histogramNamesArePrefixed() {
        Histogram histogram = MetricsRegistry.namespace("nerdbot").histogram("latency_seconds", "Latency");
        histogram.observe(0.2);

        assertThat(REGISTRY.getSampleValue("nerdbot_latency_seconds_count")).isEqualTo(1.0);
    }

    @Test
    void histogramWithBucketsUsesThemUnderThePrefix() {
        Histogram histogram = MetricsRegistry.namespace("nerdbot").histogram("size_bytes", "Size", new double[]{10, 100});
        histogram.observe(50);

        assertThat(REGISTRY.getSampleValue("nerdbot_size_bytes_bucket", new String[]{"le"}, new String[]{"100.0"})).isEqualTo(1.0);
        assertThat(REGISTRY.getSampleValue("nerdbot_size_bytes_bucket", new String[]{"le"}, new String[]{"10.0"})).isEqualTo(0.0);
    }

    @Test
    void summaryNameIsPrefixed() {
        Summary summary = MetricsRegistry.namespace("nerdbot").summary("curate_seconds", "Curation time");
        summary.observe(3);

        assertThat(REGISTRY.getSampleValue("nerdbot_curate_seconds_count")).isEqualTo(1.0);
    }

    @Test
    void sameNameInDifferentNamespacesDoesNotCollide() {
        MetricsRegistry.namespace("nerdbot").counter("events_total", "NerdBot events").inc();
        MetricsRegistry.namespace("ticketbot").counter("events_total", "TicketBot events").inc(2);

        assertThat(REGISTRY.getSampleValue("nerdbot_events_total")).isEqualTo(1.0);
        assertThat(REGISTRY.getSampleValue("ticketbot_events_total")).isEqualTo(2.0);
    }

    @Test
    void staticMethodsStayUnprefixed() {
        MetricsRegistry.counter("plain_total", "No prefix").inc();

        assertThat(REGISTRY.getSampleValue("plain_total")).isEqualTo(1.0);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "1nerdbot", "nerd-bot", "nerd bot", "nerd:bot"})
    void invalidNamespacesAreRejectedUpFront(String namespace) {
        assertThatThrownBy(() -> MetricsRegistry.namespace(namespace))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nerdbot", "_internal", "Nerd_Bot2"})
    void validNamespacesAreAccepted(String namespace) {
        assertThat(MetricsRegistry.namespace(namespace)).isNotNull();
    }
}
