package net.hypixel.nerdbot.marmalade.discord;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSplitterTest {

    @Test
    void shortMessageReturnsSingleElement() {
        List<String> result = MessageSplitter.split("Hello, world!");
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("Hello, world!");
    }

    @Test
    void emptyStringReturnsEmptyList() {
        List<String> result = MessageSplitter.split("");
        assertThat(result).isEmpty();
    }

    @Test
    void nullReturnsEmptyList() {
        List<String> result = MessageSplitter.split(null);
        assertThat(result).isEmpty();
    }

    @Test
    void splitsAtNewline() {
        // 1800 a's + newline + 400 b's = 2201 chars, forces a split
        String content = "a".repeat(1800) + "\n" + "b".repeat(400);
        List<String> result = MessageSplitter.split(content);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).endsWith("a");
        assertThat(result.get(1)).startsWith("b");
    }

    @Test
    void splitsAtSpace() {
        String word = "word ";
        String content = word.repeat(500); // 2500 chars
        List<String> result = MessageSplitter.split(content);

        assertThat(result.size()).isGreaterThan(1);
    }

    @Test
    void hardSplitsWhenNoBreakpoints() {
        String content = "a".repeat(5000);
        List<String> result = MessageSplitter.split(content);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).hasSize(2000);
        assertThat(result.get(1)).hasSize(2000);
        assertThat(result.get(2)).hasSize(1000);
    }

    @Test
    void allChunksWithinLimit() {
        String content = "word ".repeat(200); // 1000 chars
        List<String> result = MessageSplitter.split(content, 100);

        assertThat(result).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(100));
    }

    @Test
    void preservesCodeBlocks() {
        String content = "Before\n```java\n" + "x".repeat(3000) + "\n```\nAfter";
        List<String> result = MessageSplitter.splitPreservingCodeBlocks(content);

        assertThat(result.size()).isGreaterThan(1);
        assertThat(result.get(0)).endsWith("```");
        assertThat(result.get(1)).startsWith("```java\n");
    }

    @Test
    void codeBlockPreservationWithNoCodeBlocks() {
        String content = "Simple short text";
        List<String> result = MessageSplitter.splitPreservingCodeBlocks(content);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(content);
    }
}