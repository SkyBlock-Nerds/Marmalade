package net.hypixel.nerdbot.marmalade.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsRegistryTest {

    @AfterEach
    void cleanUp() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void counterNoLabels() {
        Counter counter = MetricsRegistry.counter("test_counter_no_labels", "A counter with no labels");
        counter.inc();
        assertThat(counter.get()).isEqualTo(1.0);
    }

    @Test
    void counterWithLabels() {
        Counter counter = MetricsRegistry.counter("test_counter_with_labels", "A counter with labels", "status", "method");
        counter.labels("200", "GET").inc();
        assertThat(counter.labels("200", "GET").get()).isEqualTo(1.0);
    }

    @Test
    void gaugeNoLabels() {
        Gauge gauge = MetricsRegistry.gauge("test_gauge_no_labels", "A gauge with no labels");
        gauge.set(42);
        assertThat(gauge.get()).isEqualTo(42.0);
    }

    @Test
    void gaugeWithLabels() {
        Gauge gauge = MetricsRegistry.gauge("test_gauge_with_labels", "A gauge with labels", "env");
        gauge.labels("prod").set(100);
        assertThat(gauge.labels("prod").get()).isEqualTo(100.0);
    }

    @Test
    void histogramNoLabels() {
        Histogram histogram = MetricsRegistry.histogram("test_histogram_no_labels", "A histogram with no labels");
        histogram.observe(0.5);
        assertThat(histogram.collect()).isNotEmpty();
    }

    @Test
    void histogramWithCustomBuckets() {
        Histogram histogram = MetricsRegistry.histogram(
            "test_histogram_custom_buckets",
            "A histogram with custom buckets",
            new double[]{0.1, 0.5, 1.0, 5.0},
            "path"
        );
        histogram.labels("/api").observe(0.3);
        assertThat(histogram.collect()).isNotEmpty();
    }

    @Test
    void summaryNoLabels() {
        Summary summary = MetricsRegistry.summary("test_summary_no_labels", "A summary with no labels");
        summary.observe(1.5);
        assertThat(summary.collect()).isNotEmpty();
    }

    @Test
    void summaryWithLabels() {
        Summary summary = MetricsRegistry.summary("test_summary_with_labels", "A summary with labels", "type");
        summary.labels("request").observe(2.0);
        assertThat(summary.collect()).isNotEmpty();
    }
}
