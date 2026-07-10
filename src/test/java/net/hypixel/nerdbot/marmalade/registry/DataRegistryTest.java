package net.hypixel.nerdbot.marmalade.registry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class MergeItem {
        private String name;
        private int value;
        private String icon;
        private Map<String, String> overrides;
    }

    static class MergeRegistry extends DataRegistry<MergeItem> {

        @Override
        protected Class<MergeItem[]> getArrayType() {
            return MergeItem[].class;
        }

        @Override
        protected String getResourcePath() {
            return "data/merge-test-items.json";
        }

        @Override
        protected Function<MergeItem, String> getNameExtractor() {
            return MergeItem::getName;
        }
    }

    @Nested
    class ExternalMerge {

        @TempDir
        Path tempDir;

        private MergeRegistry mergeRegistry;
        private Logger dataRegistryLogger;
        private ListAppender<ILoggingEvent> logAppender;
        private String previousDataDir;

        @BeforeEach
        void setUp() {
            mergeRegistry = new MergeRegistry();
            previousDataDir = System.getProperty("marmalade.data.dir");
            System.setProperty("marmalade.data.dir", tempDir.toString());

            dataRegistryLogger = (Logger) LoggerFactory.getLogger(DataRegistry.class);
            dataRegistryLogger.setLevel(Level.WARN);
            logAppender = new ListAppender<>();
            logAppender.start();
            dataRegistryLogger.addAppender(logAppender);
        }

        @AfterEach
        void tearDown() {
            dataRegistryLogger.detachAppender(logAppender);
            dataRegistryLogger.setLevel(null);
            if (previousDataDir == null) {
                System.clearProperty("marmalade.data.dir");
            } else {
                System.setProperty("marmalade.data.dir", previousDataDir);
            }
        }

        private void writeExternalFile(String json) throws IOException {
            Path externalFile = tempDir.resolve("data/merge-test-items.json");
            Files.createDirectories(externalFile.getParent());
            Files.writeString(externalFile, json);
        }

        private List<String> warnMessages() {
            return logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        }

        @Test
        void replacesBundledEntryByNameCaseInsensitive() throws IOException {
            writeExternalFile("""
                [{"name": "sword", "value": 99, "icon": "sword_v2.png", "overrides": {"pack": "sword_pack_v2.png"}}]
                """);
            mergeRegistry.load();

            assertThat(mergeRegistry.size()).isEqualTo(2);
            assertThat(mergeRegistry.byName("Sword")).isPresent().get()
                .satisfies(item -> {
                    assertThat(item.getValue()).isEqualTo(99);
                    assertThat(item.getIcon()).isEqualTo("sword_v2.png");
                });
        }

        @Test
        void appendsExternalEntriesWithNewNames() throws IOException {
            writeExternalFile("""
                [{"name": "Bow", "value": 7, "icon": "bow.png"}]
                """);
            mergeRegistry.load();

            assertThat(mergeRegistry.size()).isEqualTo(3);
            assertThat(mergeRegistry.byName("Bow")).isPresent();
            assertThat(warnMessages()).isEmpty();
        }

        @Test
        void warnsWhenExternalEntryDropsBundledField() throws IOException {
            writeExternalFile("""
                [{"name": "Sword", "value": 11, "icon": "sword_v2.png"}]
                """);
            mergeRegistry.load();

            assertThat(warnMessages()).hasSize(1);
            assertThat(warnMessages().get(0))
                .contains("Sword")
                .contains("overrides")
                .doesNotContain("icon");
        }

        @Test
        void warnListsEveryDroppedField() throws IOException {
            writeExternalFile("""
                [{"name": "Sword", "value": 11}]
                """);
            mergeRegistry.load();

            assertThat(warnMessages()).hasSize(1);
            assertThat(warnMessages().get(0))
                .contains("icon")
                .contains("overrides");
        }

        @Test
        void treatsExplicitNullAsDroppedField() throws IOException {
            writeExternalFile("""
                [{"name": "Sword", "value": 11, "icon": null, "overrides": {"pack": "sword_pack_v2.png"}}]
                """);
            mergeRegistry.load();

            assertThat(warnMessages()).hasSize(1);
            assertThat(warnMessages().get(0))
                .contains("icon")
                .doesNotContain("overrides");
        }

        @Test
        void doesNotWarnWhenExternalEntryRetainsAllFields() throws IOException {
            writeExternalFile("""
                [{"name": "Sword", "value": 99, "icon": "sword_v2.png", "overrides": {"pack": "sword_pack_v2.png"}}]
                """);
            mergeRegistry.load();

            assertThat(warnMessages()).isEmpty();
        }

        @Test
        void doesNotWarnWhenExternalEntryAddsFields() throws IOException {
            writeExternalFile("""
                [{"name": "Shield", "value": 6, "icon": "shield_v2.png", "overrides": {"pack": "shield_pack.png"}}]
                """);
            mergeRegistry.load();

            assertThat(warnMessages()).isEmpty();
        }

        @Test
        void cannotDetectDroppedPrimitiveFields() throws IOException {
            // 'value' is a primitive int: Gson defaults an omitted field to 0 and always serializes it,
            // so a dropped primitive is indistinguishable from an explicit 0 and is never warned about.
            writeExternalFile("""
                [{"name": "Sword", "icon": "sword_v2.png", "overrides": {"pack": "sword_pack_v2.png"}}]
                """);
            mergeRegistry.load();

            assertThat(mergeRegistry.byName("Sword")).isPresent().get()
                .extracting(MergeItem::getValue).isEqualTo(0);
            assertThat(warnMessages()).isEmpty();
        }
    }
}