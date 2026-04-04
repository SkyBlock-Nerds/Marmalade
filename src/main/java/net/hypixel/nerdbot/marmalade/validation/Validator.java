package net.hypixel.nerdbot.marmalade.validation;

import lombok.Getter;
import net.hypixel.nerdbot.marmalade.exception.FormattedException;
import net.hypixel.nerdbot.marmalade.functional.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Accumulating validator that collects all constraint violations before reporting them,
 * rather than throwing on the first failure like {@link Preconditions}.
 * Obtain an instance via {@link #create()}, chain check methods, then call {@link #validate()} or {@link #toResult()}.
 */
public class Validator {

    private final List<ValidationError> errors = new ArrayList<>();

    private Validator() {
    }

    /**
     * Creates a new, empty {@code Validator} instance.
     *
     * @return a fresh {@code Validator} with no recorded errors
     */
    public static Validator create() {
        return new Validator();
    }

    /**
     * Records a validation error if {@code value} is null.
     *
     * @param value     the value to test
     * @param fieldName the name of the field, used as the error key and in the message
     * @return this {@code Validator}, for chaining
     */
    public Validator notNull(Object value, String fieldName) {
        if (value == null) {
            errors.add(new ValidationError(fieldName, fieldName + " must not be null"));
        }
        return this;
    }

    /**
     * Records a validation error if {@code value} is null or blank (empty or whitespace-only).
     *
     * @param value     the string to test
     * @param fieldName the name of the field, used as the error key and in the message
     * @return this {@code Validator}, for chaining
     */
    public Validator notBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            errors.add(new ValidationError(fieldName, fieldName + " must not be blank"));
        }
        return this;
    }

    /**
     * Records a validation error if {@code value} is non-null and shorter than {@code min} characters.
     * A null value is silently skipped -- combine with {@link #notNull} if null should also be rejected.
     *
     * @param value     the string to test
     * @param min       the minimum allowed length (inclusive)
     * @param fieldName the name of the field, used as the error key and in the message
     * @return this {@code Validator}, for chaining
     */
    public Validator minLength(String value, int min, String fieldName) {
        if (value != null && value.length() < min) {
            errors.add(new ValidationError(fieldName, fieldName + " must be at least " + min + " characters, but was " + value.length()));
        }
        return this;
    }

    /**
     * Records a validation error if {@code value} is non-null and longer than {@code max} characters.
     * A null value is silently skipped -- combine with {@link #notNull} if null should also be rejected.
     *
     * @param value     the string to test
     * @param max       the maximum allowed length (inclusive)
     * @param fieldName the name of the field, used as the error key and in the message
     * @return this {@code Validator}, for chaining
     */
    public Validator maxLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            errors.add(new ValidationError(fieldName, fieldName + " must be at most " + max + " characters, but was " + value.length()));
        }
        return this;
    }

    /**
     * Records a validation error if {@code value} is non-null and outside the closed range [{@code min}, {@code max}].
     * A null value is silently skipped -- combine with {@link #notNull} if null should also be rejected.
     *
     * @param <T>       a {@link Comparable} type
     * @param value     the value to test
     * @param min       the lower bound (inclusive)
     * @param max       the upper bound (inclusive)
     * @param fieldName the name of the field, used as the error key and in the message
     * @return this {@code Validator}, for chaining
     */
    public <T extends Comparable<T>> Validator inRange(T value, T min, T max, String fieldName) {
        if (value != null && (value.compareTo(min) < 0 || value.compareTo(max) > 0)) {
            errors.add(new ValidationError(fieldName, fieldName + " must be between " + min + " and " + max + ", but was " + value));
        }
        return this;
    }

    /**
     * Records a validation error if {@code value} is null or empty.
     *
     * @param value     the collection to test
     * @param fieldName the name of the field, used as the error key and in the message
     * @return this {@code Validator}, for chaining
     */
    public Validator notEmpty(Collection<?> value, String fieldName) {
        if (value == null || value.isEmpty()) {
            errors.add(new ValidationError(fieldName, fieldName + " must not be empty"));
        }
        return this;
    }

    /**
     * Records a validation error with a formatted message if {@code condition} is false.
     * Placeholders in {@code message} are filled by {@code args} using {@code {}} syntax.
     * The resulting error is not associated with any specific field.
     *
     * @param condition the boolean expression that must be true
     * @param message   the message template, with {@code {}} placeholders
     * @param args      arguments substituted into the message template
     * @return this {@code Validator}, for chaining
     */
    public Validator check(boolean condition, String message, Object... args) {
        if (!condition) {
            String formatted = safeFormat(message, args);
            errors.add(new ValidationError(null, formatted));
        }
        return this;
    }

    /**
     * Throws a {@link ValidationException} containing all accumulated errors if any exist.
     *
     * @throws ValidationException if one or more validation errors have been recorded
     */
    public void validate() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Returns a {@link Result} representing the outcome of all accumulated checks.
     * Succeeds with {@code null} when there are no errors, or fails with a {@link ValidationException}.
     *
     * @return a successful {@code Result} if there are no errors, otherwise a failing one
     */
    public Result<Void, ValidationException> toResult() {
        if (errors.isEmpty()) {
            return Result.success(null);
        }
        return Result.failure(new ValidationException(errors));
    }

    /**
     * Returns an unmodifiable snapshot of all errors recorded so far.
     *
     * @return an immutable list of {@link ValidationError} instances; empty if no errors have been recorded
     */
    public List<ValidationError> getErrors() {
        return List.copyOf(errors);
    }

    /**
     * Returns {@code true} if at least one validation error has been recorded.
     *
     * @return {@code true} if there are errors, {@code false} otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
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

    /**
     * Immutable record representing a single failed validation constraint.
     *
     * @param fieldName the name of the field that failed, or {@code null} for cross-field checks
     * @param message   a human-readable description of the constraint violation
     */
    public record ValidationError(String fieldName, String message) {
    }

    /**
     * Thrown by {@link Validator#validate()} when one or more validation errors have been recorded.
     * The exception message lists every error in a human-readable format.
     */
    public static class ValidationException extends FormattedException {

        @Getter
        private final List<ValidationError> errors;

        /**
         * Constructs a {@code ValidationException} from the given list of errors.
         *
         * @param errors the non-empty list of validation errors to include; copied defensively
         */
        public ValidationException(List<ValidationError> errors) {
            super(buildMessage(errors));
            this.errors = List.copyOf(errors);
        }

        private static String buildMessage(List<ValidationError> errors) {
            StringBuilder sb = new StringBuilder("Validation failed with ");
            sb.append(errors.size());
            sb.append(errors.size() == 1 ? " error" : " errors");
            sb.append(":");
            for (ValidationError error : errors) {
                sb.append("\n  - ").append(error.message());
            }
            return sb.toString();
        }
    }
}