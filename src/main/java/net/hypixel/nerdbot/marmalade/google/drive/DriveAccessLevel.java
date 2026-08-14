package net.hypixel.nerdbot.marmalade.google.drive;

import lombok.Getter;

import java.util.Optional;

/**
 * The Drive permission roles this system manages, declared least- to
 * most-permissive so {@link #outranks(DriveAccessLevel)} can compare them.
 * Shared-drive-only roles (organizer, fileOrganizer) are deliberately absent:
 * the bot grants content access, never management access.
 */
@Getter
public enum DriveAccessLevel {
    READER("reader"),
    COMMENTER("commenter"),
    WRITER("writer");

    private final String apiRole;

    DriveAccessLevel(String apiRole) {
        this.apiRole = apiRole;
    }

    public static Optional<DriveAccessLevel> fromApiRole(String apiRole) {
        for (DriveAccessLevel level : values()) {
            if (level.apiRole.equals(apiRole)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }

    public boolean outranks(DriveAccessLevel other) {
        return this.ordinal() > other.ordinal();
    }
}
