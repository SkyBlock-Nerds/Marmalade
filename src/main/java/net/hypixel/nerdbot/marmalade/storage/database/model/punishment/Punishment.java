package net.hypixel.nerdbot.marmalade.storage.database.model.punishment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Punishment {

    private String punishmentId;
    private String targetUserId;
    private String moderatorUserId;
    private PunishmentType type;
    private String reason;
    private String notes;
    private long createdAt;

    public Punishment(String targetUserId, String moderatorUserId, PunishmentType type, String reason, String notes) {
        this.punishmentId = UUID.randomUUID().toString();
        this.targetUserId = targetUserId;
        this.moderatorUserId = moderatorUserId;
        this.type = type;
        this.reason = reason;
        this.notes = notes;
        this.createdAt = System.currentTimeMillis();
    }
}