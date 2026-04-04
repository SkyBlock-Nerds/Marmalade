package net.hypixel.nerdbot.marmalade.functional;

import java.util.function.Function;

/**
 * A {@link Function} variant whose {@link #apply(Object)} method may throw a checked exception of type {@code E}.
 * Use {@link #sneaky} or {@link #unchecked} to adapt instances for use with APIs that require a plain
 * {@code Function}.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of checked exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {

    /**
     * Applies this function to the given argument, potentially throwing a checked exception.
     *
     * @param t the function input
     * @return the function result
     * @throws E if the function is unable to produce a result
     */
    R apply(T t) throws E;

    /**
     * Wraps a {@code ThrowingFunction} in a plain {@link Function} that rethrows any checked
     * exception as an unchecked exception without wrapping it (sneaky throw).
     *
     * @param <T> the type of the input
     * @param <R> the type of the result
     * @param fn  the throwing function to wrap
     * @return a {@code Function} that propagates exceptions without declaring them
     */
    static <T, R> Function<T, R> sneaky(ThrowingFunction<T, R, ?> fn) {
        return t -> {
            try {
                return fn.apply(t);
            } catch (Exception e) {
                throw throwSneaky(e);
            }
        };
    }

    /**
     * Wraps a {@code ThrowingFunction} in a plain {@link Function} that rethrows
     * {@link RuntimeException} as-is and wraps any other checked exception in a
     * {@link RuntimeException}.
     *
     * @param <T> the type of the input
     * @param <R> the type of the result
     * @param fn  the throwing function to wrap
     * @return a {@code Function} that wraps checked exceptions in {@code RuntimeException}
     */
    static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R, ?> fn) {
        return t -> {
            try {
                return fn.apply(t);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException throwSneaky(Throwable t) throws E {
        throw (E) t;
    }
}
