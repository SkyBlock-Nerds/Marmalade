package net.hypixel.nerdbot.marmalade.storage.database.model.punishment;

import lombok.Getter;
import net.hypixel.nerdbot.marmalade.EnumUtils;

@Getter
public enum PunishmentType {
    WARNING("Warning", "A formal warning issued to the user"),
    KICK("Kick", "User was kicked from the server"),
    BAN("Ban", "User was banned from the server"),
    DEMOTION("Demotion", "User was demoted from their role"),
    MUTE("Mute", "User was muted in the server"),
    OTHER("Other", "A custom moderation note or action");

    private static final PunishmentType[] VALUES = values();
    private final String displayName;
    private final String description;

    PunishmentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public static PunishmentType fromName(String name) {
        if (name == null) {
            return null;
        }

        return EnumUtils.findValue(VALUES, name);
    }
}
