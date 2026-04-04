package net.hypixel.nerdbot.marmalade.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormattedExceptionTest {

    @Test
    void formatsPlaceholdersLeftToRight() {
        FormattedException ex = new FormattedException("Hello {} and {}", "Alice", "Bob");
        assertThat(ex.getMessage()).isEqualTo("Hello Alice and Bob");
    }

    @Test
    void handlesNoPlaceholders() {
        FormattedException ex = new FormattedException("No placeholders here");
        assertThat(ex.getMessage()).isEqualTo("No placeholders here");
    }

    @Test
    void handlesNoArgs() {
        FormattedException ex = new FormattedException("Has {} but no args");
        assertThat(ex.getMessage()).isEqualTo("Has {} but no args");
    }

    @Test
    void handlesExcessArgs() {
        FormattedException ex = new FormattedException("Only {}", "used", "ignored");
        assertThat(ex.getMessage()).isEqualTo("Only used");
    }

    @Test
    void handlesNullArgs() {
        FormattedException ex = new FormattedException("Value is {}", (Object) null);
        assertThat(ex.getMessage()).isEqualTo("Value is null");
    }

    @Test
    void handlesNullMessage() {
        FormattedException ex = new FormattedException(null);
        assertThat(ex.getMessage()).isNull();
    }

    @Test
    void preservesCause() {
        RuntimeException cause = new RuntimeException("root");
        FormattedException ex = new FormattedException("Failed: {}", cause, "details");
        assertThat(ex.getMessage()).isEqualTo("Failed: details");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void causeOnlyConstructor() {
        RuntimeException cause = new RuntimeException("root");
        FormattedException ex = new FormattedException(cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void isRuntimeException() {
        assertThat(new FormattedException("test")).isInstanceOf(RuntimeException.class);
    }
}
