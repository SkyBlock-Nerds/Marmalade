package net.hypixel.nerdbot.marmalade.functional;

import java.util.function.Consumer;

/**
 * A {@link Consumer} variant whose {@link #accept(Object)} method may throw a checked exception of type {@code E}.
 * Use {@link #sneaky} or {@link #unchecked} to adapt instances for use with APIs that require a plain
 * {@code Consumer}.
 *
 * @param <T> the type of the input consumed
 * @param <E> the type of checked exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {

    /**
     * Performs this operation on the given argument, potentially throwing a checked exception.
     *
     * @param t the input argument
     * @throws E if the operation fails
     */
    void accept(T t) throws E;

    /**
     * Wraps a {@code ThrowingConsumer} in a plain {@link Consumer} that rethrows any checked
     * exception as an unchecked exception without wrapping it (sneaky throw).
     *
     * @param <T>      the type of the input
     * @param consumer the throwing consumer to wrap
     * @return a {@code Consumer} that propagates exceptions without declaring them
     */
    static <T> Consumer<T> sneaky(ThrowingConsumer<T, ?> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                throw throwSneaky(e);
            }
        };
    }

    /**
     * Wraps a {@code ThrowingConsumer} in a plain {@link Consumer} that rethrows
     * {@link RuntimeException} as-is and wraps any other checked exception in a
     * {@link RuntimeException}.
     *
     * @param <T>      the type of the input
     * @param consumer the throwing consumer to wrap
     * @return a {@code Consumer} that wraps checked exceptions in {@code RuntimeException}
     */
    static <T> Consumer<T> unchecked(ThrowingConsumer<T, ?> consumer) {
        return t -> {
            try {
                consumer.accept(t);
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
