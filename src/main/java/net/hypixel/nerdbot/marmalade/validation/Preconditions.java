package net.hypixel.nerdbot.marmalade.validation;

import lombok.experimental.UtilityClass;

import java.util.Collection;

/**
 * Static guard methods that throw {@link IllegalArgumentException} immediately on violation.
 * Use these at method entry points where fail-fast behaviour is desired.
 */
@UtilityClass
public class Preconditions {

    /**
     * Asserts that {@code value} is not null, using a simple field name in the error message.
     *
     * @param <T>       the type of the value being checked
     * @param value     the value to test
     * @param fieldName the name of the field, included in the exception message
     * @return {@code value} if it is not null
     * @throws IllegalArgumentException if {@code value} is null
     */
    public static <T> T notNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    /**
     * Asserts that {@code value} is not null, using a formatted message template in the error.
     * Placeholders in {@code message} are filled by {@code args} using {@code {}} syntax.
     *
     * @param <T>     the type of the value being checked
     * @param value   the value to test
     * @param message the message template, with {@code {}} placeholders
     * @param args    arguments substituted into the message template
     * @return {@code value} if it is not null
     * @throws IllegalArgumentException if {@code value} is null
     */
    public static <T> T notNull(T value, String message, Object... args) {
        if (value == null) {
            throw new IllegalArgumentException(safeFormat(message, args));
        }
        return value;
    }

    /**
     * Asserts that {@code value} is neither null nor blank (empty or whitespace-only).
     *
     * @param value     the string to test
     * @param fieldName the name of the field, included in the exception message
     * @return {@code value} if it is not blank
     * @throws IllegalArgumentException if {@code value} is null or blank
     */
    public static String notBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    /**
     * Asserts that {@code value} is at least {@code min} characters long.
     *
     * @param value     the string to test; must not be null
     * @param min       the minimum allowed length (inclusive)
     * @param fieldName the name of the field, included in the exception message
     * @return {@code value} if its length satisfies the constraint
     * @throws IllegalArgumentException if {@code value} is null or shorter than {@code min}
     */
    public static String minLength(String value, int min, String fieldName) {
        notNull(value, fieldName);
        if (value.length() < min) {
            throw new IllegalArgumentException(fieldName + " must be at least " + min + " characters, but was " + value.length());
        }
        return value;
    }

    /**
     * Asserts that {@code value} is at most {@code max} characters long.
     *
     * @param value     the string to test; must not be null
     * @param max       the maximum allowed length (inclusive)
     * @param fieldName the name of the field, included in the exception message
     * @return {@code value} if its length satisfies the constraint
     * @throws IllegalArgumentException if {@code value} is null or longer than {@code max}
     */
    public static String maxLength(String value, int max, String fieldName) {
        notNull(value, fieldName);
        if (value.length() > max) {
            throw new IllegalArgumentException(fieldName + " must be at most " + max + " characters, but was " + value.length());
        }
        return value;
    }

    /**
     * Asserts that {@code value} falls within the closed range [{@code min}, {@code max}].
     *
     * @param <T>       a {@link Comparable} type
     * @param value     the value to test; must not be null
     * @param min       the lower bound (inclusive)
     * @param max       the upper bound (inclusive)
     * @param fieldName the name of the field, included in the exception message
     * @return {@code value} if it is within range
     * @throws IllegalArgumentException if {@code value} is null or outside the range
     */
    public static <T extends Comparable<T>> T inRange(T value, T min, T max, String fieldName) {
        notNull(value, fieldName);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between " + min + " and " + max + ", but was " + value);
        }
        return value;
    }

    /**
     * Asserts that {@code value} is strictly greater than zero.
     *
     * @param value     the integer to test
     * @param fieldName the name of the field, included in the exception message
     * @return {@code value} if it is positive
     * @throws IllegalArgumentException if {@code value} is zero or negative
     */
    public static int positive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive, but was " + value);
        }
        return value;
    }

    /**
     * Asserts that {@code value} is neither null nor empty.
     *
     * @param <C>       a {@link Collection} type
     * @param value     the collection to test
     * @param fieldName the name of the field, included in the exception message
     * @return {@code value} if it is not empty
     * @throws IllegalArgumentException if {@code value} is null or empty
     */
    public static <C extends Collection<?>> C notEmpty(C value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return value;
    }

    /**
     * Asserts that {@code condition} is true, throwing with a formatted message if not.
     * Placeholders in {@code message} are filled by {@code args} using {@code {}} syntax.
     *
     * @param condition the boolean expression that must be true
     * @param message   the message template, with {@code {}} placeholders
     * @param args      arguments substituted into the message template
     * @throws IllegalArgumentException if {@code condition} is false
     */
    public static void check(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(safeFormat(message, args));
        }
    }

    private static String safeFormat(String message, Object... args) {
        if (message == null) {
            return null;
        }

        if (args == null || args.length == 0) {
            return message;
        }

        StringBuilder sb = new StringBuilder(message.length() + 32);
        int argIndex = 0;
        int i = 0;

        while (i < message.length()) {
            if (i + 1 < message.length() && message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(String.valueOf(args[argIndex++]));
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(message.charAt(i));
                i++;
            }
        }

        return sb.toString();
    }
}