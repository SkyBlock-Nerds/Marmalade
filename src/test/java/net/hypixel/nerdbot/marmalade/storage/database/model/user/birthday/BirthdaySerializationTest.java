package net.hypixel.nerdbot.marmalade.storage.database.model.user.birthday;

import net.hypixel.nerdbot.marmalade.json.DataSerialization;
import net.hypixel.nerdbot.marmalade.storage.database.model.user.DiscordUser;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BirthdaySerializationTest {

    private static String store(DiscordUser user) {
        return Document.parse(DataSerialization.GSON.toJson(user)).toJson();
    }

    private static BirthdayData load(String storedJson) {
        return DataSerialization.GSON.fromJson(storedJson, DiscordUser.class).getBirthdayData();
    }

    @Test
    void setBirthdaySurvivesARoundTrip() {
        DiscordUser user = new DiscordUser("1");
        user.setBirthday(LocalDate.of(2007, 10, 4), LocalDate.of(2026, 10, 11));
        user.getBirthdayData().setShouldAnnounceAge(true);
        user.getBirthdayData().setTimezone("Europe/London");
        user.getBirthdayData().setLastAnnouncedYear(2026);

        String stored = store(user);
        BirthdayData loaded = load(stored);

        assertThat(stored).contains("\"birthDate\": \"2007-10-04\"").doesNotContain("birthdayTimestamp");
        assertThat(loaded.getBirthDate()).isEqualTo(LocalDate.of(2007, 10, 4));
        assertThat(loaded.isShouldAnnounceAge()).isTrue();
        assertThat(loaded.getTimezone()).isEqualTo("Europe/London");
        assertThat(loaded.getLastAnnouncedYear()).isEqualTo(2026);
    }

    @Test
    void unsetBirthdayIsOmittedAndStaysUnset() {
        String stored = store(new DiscordUser("1"));

        assertThat(stored).doesNotContain("birthDate");
        assertThat(load(stored).isBirthdaySet()).isFalse();
    }

    @Test
    void legacyTimestampDocumentsLoadAsDates() {
        assertThat(load("{\"discordId\": \"1\", \"birthdayData\": {\"birthdayTimestamp\": 1191456000000}}")
            .getBirthDate()).isEqualTo(LocalDate.of(2007, 10, 4));
        assertThat(load("{\"discordId\": \"1\", \"birthdayData\": {\"birthdayTimestamp\": "
            + "{\"$numberLong\": \"1191456000000\"}, \"timezone\": \"Asia/Tokyo\"}}").getTimezone())
            .isEqualTo("Asia/Tokyo");
        assertThat(load("{\"discordId\": \"1\", \"birthdayData\": {\"birthday\": "
            + "{\"$date\": \"2001-05-20T00:00:00Z\"}}}").getBirthDate()).isEqualTo(LocalDate.of(2001, 5, 20));
    }

    @Test
    void legacyUnsetSentinelLoadsAsUnset() {
        assertThat(load("{\"discordId\": \"1\", \"birthdayData\": {\"birthdayTimestamp\": -1}}").isBirthdaySet())
            .isFalse();
    }

    @Test
    void resavingALegacyDocumentWritesTheNewField() {
        DiscordUser legacy = DataSerialization.GSON.fromJson(
            "{\"discordId\": \"1\", \"birthdayData\": {\"birthdayTimestamp\": 1191456000000}}", DiscordUser.class);

        assertThat(store(legacy)).contains("\"birthDate\": \"2007-10-04\"").doesNotContain("birthdayTimestamp");
    }
}
