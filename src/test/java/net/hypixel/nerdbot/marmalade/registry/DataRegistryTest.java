package net.hypixel.nerdbot.marmalade.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataRegistryTest {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestItem {
        private String name;
        private int value;
    }

    static class TestRegistry extends DataRegistry<TestItem> {

        @Override
        protected Class<TestItem[]> getArrayType() {
            return TestItem[].class;
        }

        @Override
        protected String getResourcePath() {
            return "data/test-items.json";
        }

        @Override
        protected Function<TestItem, String> getNameExtractor() {
            return TestItem::getName;
        }
    }

    private TestRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TestRegistry();
    }

    @Test
    void loadsFromClasspath() throws IOException {
        registry.load();
        assertThat(registry.size()).isEqualTo(3);
    }

    @Test
    void byNameCaseInsensitive() throws IOException {
        registry.load();
        assertThat(registry.byName("alpha")).isPresent().get()
            .extracting(TestItem::getName).isEqualTo("Alpha");
        assertThat(registry.byName("BETA")).isPresent().get()
            .extracting(TestItem::getName).isEqualTo("Beta");
        assertThat(registry.byName("Gamma")).isPresent().get()
            .extracting(TestItem::getName).isEqualTo("Gamma");
    }

    @Test
    void byNameReturnsEmptyForMissing() throws IOException {
        registry.load();
        assertThat(registry.byName("nonexistent")).isEmpty();
    }

    @Test
    void findFirstByPredicate() throws IOException {
        registry.load();
        Optional<TestItem> result = registry.findFirst(item -> item.getValue() > 1);
        assertThat(result).isPresent().get()
            .extracting(TestItem::getName).isEqualTo("Beta");
    }

    @Test
    void findAllByPredicate() throws IOException {
        registry.load();
        List<TestItem> results = registry.findAll(item -> item.getValue() >= 2);
        assertThat(results).hasSize(2)
            .extracting(TestItem::getName)
            .containsExactlyInAnyOrder("Beta", "Gamma");
    }

    @Test
    void getAllReturnsUnmodifiableCopy() throws IOException {
        registry.load();
        List<TestItem> all = registry.getAll();
        assertThat(all).hasSize(3);
        assertThatThrownBy(() -> all.add(new TestItem("Delta", 4)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void isEmptyBeforeLoad() {
        assertThat(registry.isEmpty()).isTrue();
        assertThat(registry.size()).isEqualTo(0);
    }
}