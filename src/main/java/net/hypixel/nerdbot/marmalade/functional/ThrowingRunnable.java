package net.hypixel.nerdbot.marmalade.functional;

/**
 * A {@link Runnable} variant whose {@link #run()} method may throw a checked exception of type {@code E}.
 * Use {@link #sneaky} or {@link #unchecked} to adapt instances for use with APIs that require a plain
 * {@code Runnable}.
 *
 * @param <E> the type of checked exception that may be thrown
 */
@FunctionalInterface
public interface ThrowingRunnable<E extends Exception> {

    /**
     * Executes this runnable, potentially throwing a checked exception.
     *
     * @throws E if execution fails
     */
    void run() throws E;

    /**
     * Wraps a {@code ThrowingRunnable} in a plain {@link Runnable} that rethrows any checked
     * exception as an unchecked exception without wrapping it (sneaky throw).
     *
     * @param runnable the throwing runnable to wrap
     * @return a {@code Runnable} that propagates exceptions without declaring them
     */
    static Runnable sneaky(ThrowingRunnable<?> runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw throwSneaky(e);
            }
        };
    }

    /**
     * Wraps a {@code ThrowingRunnable} in a plain {@link Runnable} that rethrows
     * {@link RuntimeException} as-is and wraps any other checked exception in a
     * {@link RuntimeException}.
     *
     * @param runnable the throwing runnable to wrap
     * @return a {@code Runnable} that wraps checked exceptions in {@code RuntimeException}
     */
    static Runnable unchecked(ThrowingRunnable<?> runnable) {
        return () -> {
            try {
                runnable.run();
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
