package net.hypixel.nerdbot.marmalade.pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuilderTest {

    private static class NameBuilder extends Builder<String> {

        private String name;

        public NameBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        protected void validate() {
            if (name == null || name.isBlank()) {
                throw new IllegalStateException("name must not be null or blank");
            }
        }

        @Override
        protected String construct() {
            return "Hello, " + name + "!";
        }
    }

    @Test
    void buildCallsValidateThenConstruct() {
        String result = new NameBuilder().name("World").build();
        assertThat(result).isEqualTo("Hello, World!");
    }

    @Test
    void buildThrowsOnInvalidState() {
        assertThatThrownBy(() -> new NameBuilder().build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buildThrowsOnBlankName() {
        assertThatThrownBy(() -> new NameBuilder().name("   ").build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("name must not be null or blank");
    }
}
