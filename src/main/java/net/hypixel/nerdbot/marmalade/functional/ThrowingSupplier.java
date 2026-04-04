package net.hypixel.nerdbot.marmalade.functional;

import java.util.function.Supplier;

/**
 * A {@link Supplier} variant whose {@link #get()} method may throw a checked exception of type {@code E}.
 * Use {@link #sneaky} or {@link #unchecked} to adapt instances for use with APIs that require a plain
 * {@code Supplier}.
 *
 * @param <T> the type of value supplied
 * @param <E> the type of checked exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {

    /**
     * Produces a value, potentially throwing a checked exception.
     *
     * @return the supplied value
     * @throws E if unable to produce a value
     */
    T get() throws E;

    /**
     * Wraps a {@code ThrowingSupplier} in a plain {@link Supplier} that rethrows any checked
     * exception as an unchecked exception without wrapping it (sneaky throw).
     *
     * @param <T>      the type of value supplied
     * @param supplier the throwing supplier to wrap
     * @return a {@code Supplier} that propagates exceptions without declaring them
     */
    static <T> Supplier<T> sneaky(ThrowingSupplier<T, ?> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw throwSneaky(e);
            }
        };
    }

    /**
     * Wraps a {@code ThrowingSupplier} in a plain {@link Supplier} that rethrows
     * {@link RuntimeException} as-is and wraps any other checked exception in a
     * {@link RuntimeException}.
     *
     * @param <T>      the type of value supplied
     * @param supplier the throwing supplier to wrap
     * @return a {@code Supplier} that wraps checked exceptions in {@code RuntimeException}
     */
    static <T> Supplier<T> unchecked(ThrowingSupplier<T, ?> supplier) {
        return () -> {
            try {
                return supplier.get();
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
