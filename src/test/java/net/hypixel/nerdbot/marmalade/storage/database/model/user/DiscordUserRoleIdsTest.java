package net.hypixel.nerdbot.marmalade.storage.database.model.user;

import net.hypixel.nerdbot.marmalade.json.DataSerialization;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordUserRoleIdsTest {

    @Test
    void convenienceConstructorInitializesEmptyRoleIds() {
        DiscordUser user = new DiscordUser("123456789012345678");
        assertThat(user.getRoleIds()).isNotNull().isEmpty();
    }

    @Test
    void roleIdsRoundTripThroughGson() {
        DiscordUser user = new DiscordUser("123456789012345678");
        user.setRoleIds(List.of("100", "200"));

        String json = DataSerialization.GSON.toJson(user);
        assertThat(json).contains("\"roleIds\"");

        DiscordUser reloaded = DataSerialization.GSON.fromJson(json, DiscordUser.class);
        assertThat(reloaded.getRoleIds()).containsExactly("100", "200");
    }

    @Test
    void legacyDocumentWithoutRoleIdsDeserializesToNull() {
        DiscordUser reloaded = DataSerialization.GSON.fromJson("{\"discordId\":\"123\"}", DiscordUser.class);
        assertThat(reloaded.getRoleIds()).isNull();
    }
}
