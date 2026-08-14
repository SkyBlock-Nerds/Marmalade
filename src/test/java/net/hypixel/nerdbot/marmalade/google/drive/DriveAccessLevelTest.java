package net.hypixel.nerdbot.marmalade.google.drive;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DriveAccessLevelTest {

    @Test
    void mapsToAndFromApiRoles() {
        assertThat(DriveAccessLevel.READER.getApiRole()).isEqualTo("reader");
        assertThat(DriveAccessLevel.COMMENTER.getApiRole()).isEqualTo("commenter");
        assertThat(DriveAccessLevel.WRITER.getApiRole()).isEqualTo("writer");
        assertThat(DriveAccessLevel.fromApiRole("writer")).contains(DriveAccessLevel.WRITER);
        assertThat(DriveAccessLevel.fromApiRole("organizer")).isEqualTo(Optional.empty());
        assertThat(DriveAccessLevel.fromApiRole(null)).isEqualTo(Optional.empty());
    }

    @Test
    void ranksPermissiveness() {
        assertThat(DriveAccessLevel.WRITER.outranks(DriveAccessLevel.READER)).isTrue();
        assertThat(DriveAccessLevel.COMMENTER.outranks(DriveAccessLevel.WRITER)).isFalse();
        assertThat(DriveAccessLevel.READER.outranks(DriveAccessLevel.READER)).isFalse();
    }
}
