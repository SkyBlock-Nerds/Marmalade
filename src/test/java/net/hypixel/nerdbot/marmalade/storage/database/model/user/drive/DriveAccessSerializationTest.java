package net.hypixel.nerdbot.marmalade.storage.database.model.user.drive;

import net.hypixel.nerdbot.marmalade.json.DataSerialization;
import net.hypixel.nerdbot.marmalade.storage.database.model.user.DiscordUser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DriveAccessSerializationTest {

    @Test
    void roundTripsThroughRepositoryGson() {
        DiscordUser user = new DiscordUser("123");
        DriveAccess access = new DriveAccess("ciphertext", "hash");
        access.setGrants(List.of(new DriveGrant("folder-1", "perm-1", "WRITER")));
        access.setLastSyncedAt(1_755_000_000_000L);
        user.setDriveAccess(access);

        String json = DataSerialization.GSON.toJson(user);
        DiscordUser restored = DataSerialization.GSON.fromJson(json, DiscordUser.class);

        assertThat(restored.getDriveAccess()).isNotNull();
        assertThat(restored.getDriveAccess().getEncryptedEmail()).isEqualTo("ciphertext");
        assertThat(restored.getDriveAccess().getEmailHash()).isEqualTo("hash");
        assertThat(restored.getDriveAccess().getGrants())
            .containsExactly(new DriveGrant("folder-1", "perm-1", "WRITER"));
        assertThat(restored.getDriveAccess().getLastSyncedAt()).isEqualTo(1_755_000_000_000L);
    }

    @Test
    void unlinkedUserStaysNullThroughSerialization() {
        DiscordUser user = new DiscordUser("123");
        String json = DataSerialization.GSON.toJson(user);
        assertThat(json).doesNotContain("driveAccess");
        assertThat(DataSerialization.GSON.fromJson(json, DiscordUser.class).getDriveAccess()).isNull();
    }
}
