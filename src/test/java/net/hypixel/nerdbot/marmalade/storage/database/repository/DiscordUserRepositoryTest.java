package net.hypixel.nerdbot.marmalade.storage.database.repository;

import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordUserRepositoryTest {

    @Test
    void birthdayFilterMatchesStoredDateStringsOnly() {
        assertThat(DiscordUserRepository.birthdaySetFilter().toBsonDocument())
            .isEqualTo(BsonDocument.parse("{\"birthdayData.birthDate\": {\"$type\": 2}}"));
    }
}
