package net.hypixel.nerdbot.marmalade.format;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilsExtensionsTest {

    @Test
    void fromEpochMillisAndBack() {
        long millis = 1700000000000L;
        Instant instant = TimeUtils.fromEpochMillis(millis);
        assertThat(TimeUtils.toEpochMillis(instant)).isEqualTo(millis);
    }

    @Test
    void fromEpochSecondsAndBack() {
        long seconds = 1700000000L;
        Instant instant = TimeUtils.fromEpochSeconds(seconds);
        assertThat(TimeUtils.toEpochSeconds(instant)).isEqualTo(seconds);
    }

    @Test
    void formatDateTimeUsesExpectedPattern() {
        Instant instant = Instant.parse("2024-06-15T14:30:45Z");
        assertThat(TimeUtils.formatDateTime(instant)).isEqualTo("2024-06-15 14:30:45");
    }

    @Test
    void formatDateUsesExpectedPattern() {
        Instant instant = Instant.parse("2024-06-15T14:30:45Z");
        assertThat(TimeUtils.formatDate(instant)).isEqualTo("2024-06-15");
    }

    @Test
    void formatTimeUsesExpectedPattern() {
        Instant instant = Instant.parse("2024-06-15T14:30:45Z");
        assertThat(TimeUtils.formatTime(instant)).isEqualTo("14:30:45");
    }

    @Test
    void formatWithCustomFormatterAndZone() {
        Instant instant = Instant.parse("2024-06-15T14:30:45Z");
        String result = TimeUtils.format(instant, TimeUtils.DATE_TIME, ZoneOffset.ofHours(2));
        assertThat(result).isEqualTo("2024-06-15 16:30:45");
    }

    @Test
    void formatDefaultsToUtc() {
        Instant instant = Instant.parse("2024-06-15T14:30:45Z");
        String result = TimeUtils.format(instant, TimeUtils.DATE_TIME);
        assertThat(result).isEqualTo("2024-06-15 14:30:45");
    }

    @Test
    void isoCompactFormatter() {
        Instant instant = Instant.parse("2024-06-15T14:30:45Z");
        String result = TimeUtils.format(instant, TimeUtils.ISO_COMPACT);
        assertThat(result).isEqualTo("20240615T143045");
    }
}
