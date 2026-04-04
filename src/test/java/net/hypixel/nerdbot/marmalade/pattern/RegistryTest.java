package net.hypixel.nerdbot.marmalade.pattern;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryTest {

    @Test
    void registerAndGet() {
        Registry<String, Integer> registry = new Registry<>();
        registry.register("key", 42);
        assertThat(registry.get("key")).contains(42);
    }

    @Test
    void getReturnsEmptyForMissing() {
        Registry<String, Integer> registry = new Registry<>();
        assertThat(registry.get("missing")).isEmpty();
    }

    @Test
    void getOrThrowOnMissing() {
        Registry<String, Integer> registry = new Registry<>();
        assertThatThrownBy(() -> registry.getOrThrow("missing"))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void duplicateThrowsByDefault() {
        Registry<String, Integer> registry = new Registry<>();
        registry.register("key", 1);
        assertThatThrownBy(() -> registry.register("key", 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("key");
    }

    @Test
    void duplicateOverwritePolicy() {
        Registry<String, Integer> registry = new Registry<>();
        registry.register("key", 1);
        registry.register("key", 2, Registry.DuplicatePolicy.OVERWRITE);
        assertThat(registry.get("key")).contains(2);
    }

    @Test
    void duplicateIgnorePolicy() {
        Registry<String, Integer> registry = new Registry<>();
        registry.register("key", 1);
        registry.register("key", 2, Registry.DuplicatePolicy.IGNORE);
        assertThat(registry.get("key")).contains(1);
    }

    @Test
    void caseInsensitiveKeys() {
        Registry<String, Integer> registry = new Registry<>(true);
        registry.register("Key", 42);
        assertThat(registry.get("KEY")).contains(42);
        assertThat(registry.get("key")).contains(42);
        assertThat(registry.get("Key")).contains(42);
    }

    @Test
    void caseInsensitiveThrowsOnNonStringConstruction() {
        Registry<Integer, String> registry = new Registry<>(true);
        assertThatThrownBy(() -> registry.register(1, "value"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAllReturnsAllValues() {
        Registry<String, Integer> registry = new Registry<>();
        registry.register("a", 1);
        registry.register("b", 2);
        registry.register("c", 3);
        assertThat(registry.getAll()).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void keysReturnsAllKeys() {
        Registry<String, Integer> registry = new Registry<>();
        registry.register("x", 10);
        registry.register("y", 20);
        assertThat(registry.keys()).containsExactlyInAnyOrder("x", "y");
    }

    @Test
    void sizeAndIsEmpty() {
        Registry<String, Integer> registry = new Registry<>();
        assertThat(registry.isEmpty()).isTrue();
        assertThat(registry.size()).isEqualTo(0);
        registry.register("a", 1);
        assertThat(registry.isEmpty()).isFalse();
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    void removeReturnsValue() {
        Registry<String, Integer> registry = new Registry<>();
        registry.register("key", 99);
        Optional<Integer> removed = registry.remove("key");
        assertThat(removed).contains(99);
        assertThat(registry.contains("key")).isFalse();
    }

    @Test
    void removeReturnsEmptyForMissing() {
        Registry<String, Integer> registry = new Registry<>();
        assertThat(registry.remove("ghost")).isEmpty();
    }

    @Test
    void clearRemovesAll() {
        Registry<String, Integer> registry = new Registry<>();
        registry.register("a", 1);
        registry.register("b", 2);
        registry.clear();
        assertThat(registry.isEmpty()).isTrue();
        assertThat(registry.size()).isEqualTo(0);
    }

    @Test
    void threadSafeConcurrentRegistration() throws InterruptedException {
        Registry<String, Integer> registry = new Registry<>(Registry.DuplicatePolicy.IGNORE);
        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int value = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    registry.register("key-" + value, value);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(doneLatch.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(registry.size()).isEqualTo(threadCount);
    }
}
