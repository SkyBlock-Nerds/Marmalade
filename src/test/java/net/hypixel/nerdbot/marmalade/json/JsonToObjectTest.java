package net.hypixel.nerdbot.marmalade.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonToObjectTest {

    static class Settings {
        String name;
        int port;
    }

    @TempDir
    Path dir;

    @Test
    void readsTheFileIntoTheGivenClass() throws Exception {
        Path file = Files.writeString(dir.resolve("config.json"), "{\"name\":\"nerdbot\",\"port\":9190}", StandardCharsets.UTF_8);

        Settings settings = (Settings) JsonUtils.jsonToObject(file.toFile(), Settings.class);

        assertThat(settings.name).isEqualTo("nerdbot");
        assertThat(settings.port).isEqualTo(9190);
    }

    @Test
    void closesTheFileSoItCanBeReplacedRightAway() throws Exception {
        Path file = Files.writeString(dir.resolve("config.json"), "{\"name\":\"a\"}", StandardCharsets.UTF_8);

        JsonUtils.jsonToObject(file.toFile(), Settings.class);

        // Windows refuses to delete a file that still has an open handle
        Files.delete(file);
        assertThat(file).doesNotExist();
    }

    @Test
    void readsUtf8() throws Exception {
        Path file = Files.writeString(dir.resolve("config.json"), "{\"name\":\"Nérd ✓\"}", StandardCharsets.UTF_8);

        assertThat(((Settings) JsonUtils.jsonToObject(file.toFile(), Settings.class)).name).isEqualTo("Nérd ✓");
    }

    @Test
    void aMissingFileIsStillFileNotFound() {
        assertThatThrownBy(() -> JsonUtils.jsonToObject(dir.resolve("missing.json").toFile(), Settings.class))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageContaining("missing.json");
    }
}
