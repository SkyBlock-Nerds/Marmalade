package net.hypixel.nerdbot.marmalade.functional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PairTest {

    @Test
    void constructionAndAccess() {
        Pair<String, Integer> pair = Pair.of("hello", 42);
        assertThat(pair.first()).isEqualTo("hello");
        assertThat(pair.second()).isEqualTo(42);
    }

    @Test
    void mapFirst() {
        Pair<String, Integer> pair = Pair.of("hello", 42);
        Pair<Integer, Integer> mapped = pair.mapFirst(String::length);
        assertThat(mapped.first()).isEqualTo(5);
        assertThat(mapped.second()).isEqualTo(42);
    }

    @Test
    void mapSecond() {
        Pair<String, Integer> pair = Pair.of("hello", 42);
        Pair<String, String> mapped = pair.mapSecond(String::valueOf);
        assertThat(mapped.first()).isEqualTo("hello");
        assertThat(mapped.second()).isEqualTo("42");
    }

    @Test
    void allowsNullValues() {
        Pair<String, String> pair = Pair.of(null, null);
        assertThat(pair.first()).isNull();
        assertThat(pair.second()).isNull();
    }

    @Test
    void equalityAndHashCode() {
        Pair<String, Integer> a = Pair.of("x", 1);
        Pair<String, Integer> b = Pair.of("x", 1);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
