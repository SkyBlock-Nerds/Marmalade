package net.hypixel.nerdbot.marmalade.functional;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThrowingFunctionalTest {

    @Test
    void sneakySupplierRethrowsChecked() {
        assertThatThrownBy(() -> ThrowingSupplier.sneaky(() -> {
            throw new IOException("boom");
        }).get()).isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    void uncheckedSupplierWrapsInRuntime() {
        assertThatThrownBy(() -> ThrowingSupplier.unchecked(() -> {
            throw new IOException("boom");
        }).get()).isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void sneakySupplierPassesThroughValue() {
        String result = ThrowingSupplier.sneaky(() -> "hello").get();
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void sneakyFunctionRethrowsChecked() {
        assertThatThrownBy(() -> ThrowingFunction.sneaky(s -> {
            throw new IOException("boom");
        }).apply("input")).isInstanceOf(IOException.class);
    }

    @Test
    void uncheckedFunctionWrapsInRuntime() {
        assertThatThrownBy(() -> ThrowingFunction.unchecked(s -> {
            throw new IOException("boom");
        }).apply("input")).isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void sneakyFunctionWorksInStream() {
        List<String> result = Stream.of("a", "b", "c")
                .map(ThrowingFunction.sneaky(s -> s.toUpperCase()))
                .toList();
        assertThat(result).containsExactly("A", "B", "C");
    }

    @Test
    void sneakyConsumerRethrowsChecked() {
        assertThatThrownBy(() -> ThrowingConsumer.sneaky(s -> {
            throw new IOException("boom");
        }).accept("input")).isInstanceOf(IOException.class);
    }

    @Test
    void uncheckedConsumerWrapsInRuntime() {
        assertThatThrownBy(() -> ThrowingConsumer.unchecked(s -> {
            throw new IOException("boom");
        }).accept("input")).isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void sneakyConsumerPassesThrough() {
        AtomicBoolean called = new AtomicBoolean(false);
        ThrowingConsumer.sneaky(s -> called.set(true)).accept("input");
        assertThat(called).isTrue();
    }

    @Test
    void sneakyRunnableRethrowsChecked() {
        assertThatThrownBy(() -> ThrowingRunnable.sneaky(() -> {
            throw new IOException("boom");
        }).run()).isInstanceOf(IOException.class);
    }

    @Test
    void uncheckedRunnableWrapsInRuntime() {
        assertThatThrownBy(() -> ThrowingRunnable.unchecked(() -> {
            throw new IOException("boom");
        }).run()).isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void sneakyRunnablePassesThrough() {
        AtomicBoolean called = new AtomicBoolean(false);
        ThrowingRunnable.sneaky(() -> called.set(true)).run();
        assertThat(called).isTrue();
    }

    @Test
    void uncheckedSupplierDoesNotDoubleWrapRuntime() {
        RuntimeException original = new IllegalStateException("already runtime");
        assertThatThrownBy(() -> ThrowingSupplier.unchecked(() -> {
            throw original;
        }).get()).isSameAs(original);
    }
}
