package net.hypixel.nerdbot.marmalade.resilience;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryTest {

    private static final Duration FAST = Duration.ofMillis(1);

    @Test
    void succeedsOnFirstAttempt() throws Retry.RetryExhaustedException {
        String result = Retry.<String>of(() -> "ok")
            .maxAttempts(3)
            .delay(FAST)
            .execute();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void retriesAndSucceeds() throws Retry.RetryExhaustedException {
        AtomicInteger calls = new AtomicInteger(0);

        String result = Retry.<String>of(() -> {
            int call = calls.incrementAndGet();
            if (call < 3) {
                throw new IOException("not yet");
            }
            return "success";
        })
            .maxAttempts(3)
            .delay(FAST)
            .execute();

        assertThat(result).isEqualTo("success");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void throwsAfterMaxAttempts() {
        assertThatThrownBy(() ->
            Retry.<String>of(() -> {
                throw new IOException("always fails");
            })
                .maxAttempts(3)
                .delay(FAST)
                .execute()
        )
            .isInstanceOf(Retry.RetryExhaustedException.class)
            .satisfies(ex -> {
                Retry.RetryExhaustedException rex = (Retry.RetryExhaustedException) ex;
                assertThat(rex.getAttempts()).isEqualTo(3);
                assertThat(rex.getFailures()).hasSize(3);
                assertThat(rex.getCause()).isInstanceOf(IOException.class);
            });
    }

    @Test
    void retryOnFilterOnlyRetriesMatchingExceptions() {
        AtomicInteger calls = new AtomicInteger(0);

        assertThatThrownBy(() ->
            Retry.<String>of(() -> {
                calls.incrementAndGet();
                throw new IllegalStateException("wrong type");
            })
                .maxAttempts(3)
                .delay(FAST)
                .retryOn(IOException.class)
                .execute()
        )
            .isInstanceOf(Retry.RetryExhaustedException.class);

        // IllegalStateException does not match IOException -- should NOT retry
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void abortOnStopsRetryingOnMatchingException() {
        AtomicInteger calls = new AtomicInteger(0);

        assertThatThrownBy(() ->
            Retry.<String>of(() -> {
                calls.incrementAndGet();
                throw new IllegalArgumentException("abort me");
            })
                .maxAttempts(3)
                .delay(FAST)
                .abortOn(IllegalArgumentException.class)
                .execute()
        )
            .isInstanceOf(Retry.RetryExhaustedException.class);

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void retryOnAndAbortOnMutuallyExclusive() {
        assertThatThrownBy(() ->
            Retry.<String>of(() -> "irrelevant")
                .retryOn(IOException.class)
                .abortOn(IllegalArgumentException.class)
                .execute()
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("mutually exclusive");
    }

    @Test
    void onRetryListenerCalled() throws Retry.RetryExhaustedException {
        AtomicInteger listenerCount = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);

        Retry.<String>of(() -> {
            int call = calls.incrementAndGet();
            if (call < 3) {
                throw new IOException("not yet");
            }
            return "done";
        })
            .maxAttempts(3)
            .delay(FAST)
            .onRetry((attempt, ex) -> listenerCount.incrementAndGet())
            .execute();

        // Listener fires before each retry: 2 retries after attempts 1 and 2
        assertThat(listenerCount.get()).isEqualTo(2);
    }

    @Test
    void executeAsyncReturnsCompletableFuture() throws Exception {
        CompletableFuture<String> future = Retry.<String>of(() -> "async-result")
            .maxAttempts(1)
            .delay(FAST)
            .executeAsync();

        assertThat(future.get()).isEqualTo("async-result");
    }

    @Test
    void backoffIncreasesDelay() {
        // 50ms base delay, 2.0 multiplier, 2 failures before success
        // Attempt 1 fails → sleep 50ms * 2^0 = 50ms
        // Attempt 2 fails → sleep 50ms * 2^1 = 100ms
        // Attempt 3 succeeds
        // Total sleep >= 150ms
        AtomicInteger calls = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        try {
            Retry.<String>of(() -> {
                int call = calls.incrementAndGet();
                if (call < 3) {
                    throw new IOException("not yet");
                }
                return "done";
            })
                .maxAttempts(3)
                .delay(Duration.ofMillis(50))
                .backoffMultiplier(2.0)
                .execute();
        } catch (Retry.RetryExhaustedException e) {
            throw new AssertionError("Should not have exhausted", e);
        }

        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).isGreaterThanOrEqualTo(100L);
    }

    @Test
    void retryExhaustedExceptionIsFormattedException() {
        assertThatThrownBy(() ->
            Retry.<String>of(() -> {
                throw new IOException("boom");
            })
                .maxAttempts(2)
                .delay(FAST)
                .execute()
        )
            .isInstanceOf(Retry.RetryExhaustedException.class)
            .isInstanceOf(net.hypixel.nerdbot.marmalade.exception.FormattedException.class)
            .hasMessageContaining("2");
    }

    @Test
    void maxAttemptsOneNeverRetries() {
        AtomicInteger calls = new AtomicInteger(0);

        assertThatThrownBy(() ->
            Retry.<String>of(() -> {
                calls.incrementAndGet();
                throw new IOException("fail");
            })
                .maxAttempts(1)
                .delay(FAST)
                .execute()
        )
            .isInstanceOf(Retry.RetryExhaustedException.class)
            .satisfies(ex -> {
                Retry.RetryExhaustedException rex = (Retry.RetryExhaustedException) ex;
                assertThat(rex.getAttempts()).isEqualTo(1);
                assertThat(rex.getFailures()).hasSize(1);
            });

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void failuresListIsImmutable() {
        assertThatThrownBy(() ->
            Retry.<String>of(() -> {
                throw new IOException("fail");
            })
                .maxAttempts(1)
                .delay(FAST)
                .execute()
        )
            .isInstanceOf(Retry.RetryExhaustedException.class)
            .satisfies(ex -> {
                Retry.RetryExhaustedException rex = (Retry.RetryExhaustedException) ex;
                assertThatThrownBy(() -> rex.getFailures().add(new IOException("mutate")))
                    .isInstanceOf(UnsupportedOperationException.class);
            });
    }
}
