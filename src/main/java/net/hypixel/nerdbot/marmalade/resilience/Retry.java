package net.hypixel.nerdbot.marmalade.resilience;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.hypixel.nerdbot.marmalade.exception.FormattedException;
import net.hypixel.nerdbot.marmalade.functional.ThrowingSupplier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

/**
 * Utility for retrying operations that may fail transiently, with configurable attempts, delays,
 * backoff, jitter, and per-exception filtering.
 */
@UtilityClass
public class Retry {

    /**
     * Creates a new {@link RetryBuilder} configured with the given supplier.
     *
     * @param supplier the operation to retry
     * @param <T> the return type of the supplier
     * @return a new {@code RetryBuilder} for further configuration
     */
    public static <T> RetryBuilder<T> of(ThrowingSupplier<T, ? extends Exception> supplier) {
        return new RetryBuilder<>(supplier);
    }

    /**
     * Executes the supplier with the specified attempt limit and inter-attempt delay, using default settings for all other options.
     *
     * @param maxAttempts the maximum number of attempts before throwing {@link RetryExhaustedException}
     * @param delay the fixed delay between attempts
     * @param supplier the operation to retry
     * @param <T> the return type of the supplier
     * @return the result of the supplier on a successful attempt
     * @throws RetryExhaustedException if all attempts fail
     */
    public static <T> T execute(int maxAttempts, Duration delay, ThrowingSupplier<T, ? extends Exception> supplier) throws RetryExhaustedException {
        return Retry.<T>of(supplier)
            .maxAttempts(maxAttempts)
            .delay(delay)
            .execute();
    }

    /**
     * Executes the supplier with the specified attempt limit and a default 1-second inter-attempt delay.
     *
     * @param maxAttempts the maximum number of attempts before throwing {@link RetryExhaustedException}
     * @param supplier the operation to retry
     * @param <T> the return type of the supplier
     * @return the result of the supplier on a successful attempt
     * @throws RetryExhaustedException if all attempts fail
     */
    public static <T> T execute(int maxAttempts, ThrowingSupplier<T, ? extends Exception> supplier) throws RetryExhaustedException {
        return execute(maxAttempts, Duration.ofSeconds(1), supplier);
    }

    /**
     * Fluent builder for configuring and executing a retryable operation, with support for attempt
     * limits, delays, exponential backoff, jitter, exception filtering, and retry listeners.
     *
     * @param <T> the return type produced by the retried supplier
     */
    public static class RetryBuilder<T> {

        private static final Random RANDOM = new Random();

        private final ThrowingSupplier<T, ? extends Exception> supplier;

        private int maxAttempts = 3;
        private Duration delay = Duration.ofSeconds(1);
        private double backoffMultiplier = 1.0;
        private Duration maxDelay = Duration.ofSeconds(30);
        private double jitterFactor = 0.0;

        private Set<Class<? extends Exception>> retryOnTypes;
        private Set<Class<? extends Exception>> abortOnTypes;
        private BiConsumer<Integer, Exception> onRetryListener;

        private RetryBuilder(ThrowingSupplier<T, ? extends Exception> supplier) {
            this.supplier = supplier;
        }

        /**
         * Sets the maximum number of attempts before the operation is considered exhausted.
         *
         * @param maxAttempts the maximum number of attempts; defaults to 3
         * @return this builder
         */
        public RetryBuilder<T> maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Sets the base delay between attempts, before any backoff multiplier or jitter is applied.
         *
         * @param delay the base inter-attempt delay; defaults to 1 second
         * @return this builder
         */
        public RetryBuilder<T> delay(Duration delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Sets the exponential backoff multiplier applied to the delay after each attempt.
         * A value of {@code 1.0} means no backoff (constant delay).
         *
         * @param backoffMultiplier the multiplier applied per attempt; defaults to 1.0
         * @return this builder
         */
        public RetryBuilder<T> backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        /**
         * Sets the upper bound on the computed inter-attempt delay, preventing unbounded backoff growth.
         *
         * @param maxDelay the maximum delay between attempts; defaults to 30 seconds
         * @return this builder
         */
        public RetryBuilder<T> maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        /**
         * Sets the jitter factor applied to randomize the computed delay, reducing thundering-herd effects.
         * A value of {@code 0.1} allows the delay to vary by up to +/-10%.
         *
         * @param jitterFactor the fractional jitter range (e.g. 0.1 for 10%); defaults to 0.0
         * @return this builder
         */
        public RetryBuilder<T> jitterFactor(double jitterFactor) {
            this.jitterFactor = jitterFactor;
            return this;
        }

        /**
         * Restricts retries to only the specified exception types; all other exceptions cause immediate failure.
         * Mutually exclusive with {@link #abortOn(Class[])}.
         *
         * @param types the exception types that are eligible for retry
         * @return this builder
         */
        @SafeVarargs
        public final RetryBuilder<T> retryOn(Class<? extends Exception>... types) {
            this.retryOnTypes = new HashSet<>(Arrays.asList(types));
            return this;
        }

        /**
         * Causes the retry loop to abort immediately if the thrown exception matches any of the specified types.
         * Mutually exclusive with {@link #retryOn(Class[])}.
         *
         * @param types the exception types that should halt retrying
         * @return this builder
         */
        @SafeVarargs
        public final RetryBuilder<T> abortOn(Class<? extends Exception>... types) {
            this.abortOnTypes = new HashSet<>(Arrays.asList(types));
            return this;
        }

        /**
         * Registers a callback invoked after each failed attempt (except the last), receiving the
         * attempt number and the exception that was thrown.
         *
         * @param listener a consumer accepting the attempt number (1-based) and the thrown exception
         * @return this builder
         */
        public RetryBuilder<T> onRetry(BiConsumer<Integer, Exception> listener) {
            this.onRetryListener = listener;
            return this;
        }

        /**
         * Executes the supplier, retrying on failure according to the configured policy.
         *
         * @return the result of the supplier on a successful attempt
         * @throws RetryExhaustedException if all attempts fail, or a non-retryable exception is thrown
         * @throws IllegalStateException if both {@link #retryOn} and {@link #abortOn} are configured
         */
        public T execute() throws RetryExhaustedException {
            if (retryOnTypes != null && abortOnTypes != null) {
                throw new IllegalStateException("retryOn and abortOn are mutually exclusive");
            }

            List<Exception> failures = new ArrayList<>();

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return supplier.get();
                } catch (Exception e) {
                    failures.add(e);

                    boolean isLastAttempt = attempt == maxAttempts;
                    if (!shouldRetry(e) || isLastAttempt) {
                        throw new RetryExhaustedException(attempt, failures);
                    }

                    if (onRetryListener != null) {
                        onRetryListener.accept(attempt, e);
                    }

                    sleepBeforeRetry(attempt);
                }
            }

            // Unreachable -- loop always either returns or throws, but satisfies the compiler
            throw new RetryExhaustedException(maxAttempts, failures);
        }

        /**
         * Executes the retry logic asynchronously on a dedicated daemon thread, shutting the thread down
         * once the future completes.
         *
         * @return a {@link CompletableFuture} that resolves with the supplier's result, or completes
         *         exceptionally with a {@link RetryExhaustedException} if all attempts fail
         */
        public CompletableFuture<T> executeAsync() {
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "retry-async");
                t.setDaemon(true);
                return t;
            });
            return executeAsync(executor).whenComplete((result, ex) -> executor.shutdown());
        }

        /**
         * Executes the retry logic asynchronously on the provided executor.
         *
         * @param executor the scheduled executor service to submit the retry task to
         * @return a {@link CompletableFuture} that resolves with the supplier's result, or completes
         *         exceptionally with a {@link RetryExhaustedException} if all attempts fail
         */
        public CompletableFuture<T> executeAsync(ScheduledExecutorService executor) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return execute();
                } catch (RetryExhaustedException e) {
                    throw e;
                }
            }, executor);
        }

        private boolean shouldRetry(Exception e) {
            if (retryOnTypes != null) {
                return retryOnTypes.stream().anyMatch(type -> type.isInstance(e));
            }
            if (abortOnTypes != null) {
                return abortOnTypes.stream().noneMatch(type -> type.isInstance(e));
            }
            return true;
        }

        private void sleepBeforeRetry(int attempt) {
            // initialDelay * (multiplier ^ (attempt - 1)), capped at maxDelay
            double rawMs = delay.toMillis() * Math.pow(backoffMultiplier, attempt - 1);
            long cappedMs = Math.min((long) rawMs, maxDelay.toMillis());

            // Apply jitter: delay * (1 + random(-jitterFactor, +jitterFactor)), floored at 0
            long sleepMs;
            if (jitterFactor != 0.0) {
                double jitter = 1.0 + (RANDOM.nextDouble() * 2 * jitterFactor - jitterFactor);
                sleepMs = Math.max(0L, (long) (cappedMs * jitter));
            } else {
                sleepMs = cappedMs;
            }

            if (sleepMs <= 0) {
                return;
            }

            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Thrown when a {@link RetryBuilder} exhausts all configured attempts without a successful result,
     * carrying the total attempt count and the full list of exceptions encountered.
     */
    public static class RetryExhaustedException extends FormattedException {

        @Getter
        private final int attempts;

        @Getter
        private final List<Exception> failures;

        /**
         * Constructs a new {@code RetryExhaustedException} with the given attempt count and failure list.
         * The most recent failure is used as the exception cause.
         *
         * @param attempts the total number of attempts that were made
         * @param failures the ordered list of exceptions thrown across all attempts
         */
        public RetryExhaustedException(int attempts, List<Exception> failures) {
            super("Retry exhausted after {} attempt(s)", failures.getLast(), attempts);
            this.attempts = attempts;
            this.failures = List.copyOf(failures);
        }
    }
}
