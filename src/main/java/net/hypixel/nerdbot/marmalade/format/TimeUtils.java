package net.hypixel.nerdbot.marmalade.format;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class TimeUtils {

    private TimeUtils() {
    }

    public static String formatMs(long ms) {
        long days = TimeUnit.MILLISECONDS.toDays(ms);
        ms -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        ms -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        ms -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
    }

    public static String formatMsCompact(long ms) {
        long days = TimeUnit.MILLISECONDS.toDays(ms);
        ms -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        ms -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        ms -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public static String formatMsLong(long ms) {
        long days = TimeUnit.MILLISECONDS.toDays(ms);
        ms -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        ms -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        ms -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(days == 1 ? " day " : " days ");
        }

        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hour " : " hours ");
        }

        if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
        }

        sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        return sb.toString().trim();
    }

    public static boolean isAprilFirst() {
        return isAprilFirst(ZoneId.systemDefault());
    }

    public static boolean isAprilFirst(ZoneId zoneId) {
        LocalDate now = LocalDate.now(zoneId);
        return now.getMonth() == Month.APRIL && now.getDayOfMonth() == 1;
    }

    public static boolean isDayOfMonth(int dayOfMonth) {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == dayOfMonth;
    }

    /** Formatter producing {@code yyyy-MM-dd HH:mm:ss} - suitable for log output and human-readable timestamps. */
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Formatter producing {@code yyyy-MM-dd} - date without a time component. */
    public static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Formatter producing {@code HH:mm:ss} - time without a date component. */
    public static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Formatter producing {@code yyyyMMdd'T'HHmmss} - compact ISO-8601-style timestamp without separators. */
    public static final DateTimeFormatter ISO_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    /**
     * Converts a Unix epoch value in milliseconds to an {@link Instant}.
     *
     * @param millis milliseconds since the Unix epoch
     * @return the corresponding {@link Instant}
     */
    public static Instant fromEpochMillis(long millis) {
        return Instant.ofEpochMilli(millis);
    }

    /**
     * Converts a Unix epoch value in whole seconds to an {@link Instant}.
     *
     * @param seconds seconds since the Unix epoch
     * @return the corresponding {@link Instant}
     */
    public static Instant fromEpochSeconds(long seconds) {
        return Instant.ofEpochSecond(seconds);
    }

    /**
     * Returns the number of whole seconds elapsed since the Unix epoch for the given {@link Instant}.
     *
     * @param instant the instant to convert
     * @return seconds since the Unix epoch
     */
    public static long toEpochSeconds(Instant instant) {
        return instant.getEpochSecond();
    }

    /**
     * Returns the number of milliseconds elapsed since the Unix epoch for the given {@link Instant}.
     *
     * @param instant the instant to convert
     * @return milliseconds since the Unix epoch
     */
    public static long toEpochMillis(Instant instant) {
        return instant.toEpochMilli();
    }

    /**
     * Formats an {@link Instant} using the given formatter at UTC.
     *
     * @param instant   the instant to format
     * @param formatter the formatter to apply
     * @return the formatted string
     */
    public static String format(Instant instant, DateTimeFormatter formatter) {
        return format(instant, formatter, ZoneOffset.UTC);
    }

    /**
     * Formats an {@link Instant} using the given formatter in the specified time zone.
     *
     * @param instant   the instant to format
     * @param formatter the formatter to apply
     * @param zone      the time zone to use when rendering the instant
     * @return the formatted string
     */
    public static String format(Instant instant, DateTimeFormatter formatter, ZoneId zone) {
        return formatter.format(instant.atZone(zone));
    }

    /**
     * Formats an {@link Instant} as {@code yyyy-MM-dd HH:mm:ss} at UTC.
     *
     * @param instant the instant to format
     * @return the formatted date-time string
     */
    public static String formatDateTime(Instant instant) {
        return format(instant, DATE_TIME);
    }

    /**
     * Formats an {@link Instant} as {@code yyyy-MM-dd} at UTC.
     *
     * @param instant the instant to format
     * @return the formatted date string
     */
    public static String formatDate(Instant instant) {
        return format(instant, DATE_ONLY);
    }

    /**
     * Formats an {@link Instant} as {@code HH:mm:ss} at UTC.
     *
     * @param instant the instant to format
     * @return the formatted time string
     */
    public static String formatTime(Instant instant) {
        return format(instant, TIME_ONLY);
    }
}
