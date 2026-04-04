package net.hypixel.nerdbot.marmalade.functional;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A discriminated union representing either a successful value of type {@code T} or a failure
 * carrying a {@code Throwable} of type {@code E}. Use the static factory methods {@link #success},
 * {@link #failure}, and {@link #of} to construct instances.
 *
 * @param <T> the type of the success value
 * @param <E> the type of the failure throwable
 */
public sealed interface Result<T, E extends Throwable> {

    /**
     * Returns {@code true} if this result represents a success.
     *
     * @return {@code true} if successful, {@code false} otherwise
     */
    boolean isSuccess();

    /**
     * Returns {@code true} if this result represents a failure.
     *
     * @return {@code true} if failed, {@code false} otherwise
     */
    boolean isFailure();

    /**
     * If successful, applies the mapping function to the value and returns a new success result;
     * otherwise propagates the failure unchanged.
     *
     * @param <U> the type of the mapped value
     * @param fn  the function to apply to the success value
     * @return a new {@code Result} with the mapped value, or the original failure
     */
    <U> Result<U, E> map(Function<T, U> fn);

    /**
     * If successful, applies the mapping function and returns its result;
     * otherwise propagates the failure unchanged.
     *
     * @param <U> the type of the value in the returned result
     * @param fn  the function to apply to the success value, returning a {@code Result}
     * @return the result returned by {@code fn}, or the original failure
     */
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> fn);

    /**
     * If this result is a failure, applies the mapping function to the error and returns a new
     * failure with the transformed error; otherwise returns the success unchanged.
     *
     * @param <F> the type of the transformed error
     * @param fn  the function to apply to the failure error
     * @return a new failure with the mapped error, or the original success
     */
    <F extends Throwable> Result<T, F> mapError(Function<E, F> fn);

    /**
     * Returns the success value, or the given default if this result is a failure.
     *
     * @param defaultValue the value to return on failure
     * @return the success value, or {@code defaultValue}
     */
    T orElse(T defaultValue);

    /**
     * Returns the success value, or the value produced by the supplier if this result is a failure.
     *
     * @param supplier the supplier to invoke on failure
     * @return the success value, or the value from {@code supplier}
     */
    T orElseGet(Supplier<T> supplier);

    /**
     * Returns the success value, or throws the contained error if this result is a failure.
     *
     * @return the success value
     * @throws E if this result is a failure
     */
    T orElseThrow() throws E;

    /**
     * Converts this result to an {@link Optional}, containing the success value if present or
     * empty if this is a failure.
     *
     * @return an {@code Optional} containing the success value, or an empty {@code Optional}
     */
    Optional<T> toOptional();

    /**
     * Returns a single-element {@link Stream} of the success value, or an empty stream on failure.
     *
     * @return a stream containing the success value, or an empty stream
     */
    Stream<T> stream();

    /**
     * Creates a successful {@code Result} wrapping the given value.
     *
     * @param <T>   the type of the success value
     * @param <E>   the type of the error (unused for success)
     * @param value the success value
     * @return a successful {@code Result}
     */
    static <T, E extends Throwable> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failed {@code Result} wrapping the given error.
     *
     * @param <T>   the type of the value (unused for failure)
     * @param <E>   the type of the error
     * @param error the error to wrap
     * @return a failed {@code Result}
     */
    static <T, E extends Throwable> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    /**
     * Executes the given {@link ThrowingSupplier} and returns a success result with its value,
     * or a failure result if the supplier throws.
     *
     * @param <T>      the type of the value produced by the supplier
     * @param <E>      the type of the exception the supplier may throw
     * @param supplier the supplier to execute
     * @return a successful {@code Result} if the supplier completes normally, otherwise a failure
     */
    @SuppressWarnings("unchecked")
    static <T, E extends Exception> Result<T, E> of(ThrowingSupplier<T, E> supplier) {
        try {
            T value = supplier.get();
            return (Result<T, E>) (Result<?, ?>) success(value);
        } catch (Exception e) {
            return (Result<T, E>) (Result<?, ?>) failure(e);
        }
    }

    /**
     * A successful {@code Result} that holds a value of type {@code T}.
     *
     * @param <T> the type of the success value
     * @param <E> the type of the error (unused)
     * @param value the success value
     */
    record Success<T, E extends Throwable>(T value) implements Result<T, E> {

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public <U> Result<U, E> map(Function<T, U> fn) {
            return new Success<>(fn.apply(value));
        }

        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> fn) {
            return fn.apply(value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <F extends Throwable> Result<T, F> mapError(Function<E, F> fn) {
            return (Result<T, F>) this;
        }

        @Override
        public T orElse(T defaultValue) {
            return value;
        }

        @Override
        public T orElseGet(Supplier<T> supplier) {
            return value;
        }

        @Override
        public T orElseThrow() {
            return value;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.ofNullable(value);
        }

        @Override
        public Stream<T> stream() {
            return Stream.of(value);
        }
    }

    /**
     * A failed {@code Result} that holds an error of type {@code E}.
     *
     * @param <T>   the type of the value (unused)
     * @param <E>   the type of the error
     * @param error the error
     */
    record Failure<T, E extends Throwable>(E error) implements Result<T, E> {

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U, E> map(Function<T, U> fn) {
            return (Result<U, E>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> fn) {
            return (Result<U, E>) this;
        }

        @Override
        public <F extends Throwable> Result<T, F> mapError(Function<E, F> fn) {
            return new Failure<>(fn.apply(error));
        }

        @Override
        public T orElse(T defaultValue) {
            return defaultValue;
        }

        @Override
        public T orElseGet(Supplier<T> supplier) {
            return supplier.get();
        }

        @Override
        public T orElseThrow() throws E {
            throw error;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public Stream<T> stream() {
            return Stream.empty();
        }
    }
}
