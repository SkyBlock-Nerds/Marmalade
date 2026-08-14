package net.hypixel.nerdbot.marmalade.storage.database.model.user.drive;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A member's Google Drive link state: their email (AES-GCM encrypted; the
 * plaintext never touches the database), a keyed HMAC of the normalized email
 * used for duplicate-link lookups, and the grants the bot currently maintains
 * in Drive. Deleting this object (unlink, leave, ban) erases all stored email
 * data for the member.
 */
@Getter
@Setter
public class DriveAccess {

    private String encryptedEmail;
    private String emailHash;
    private List<DriveGrant> grants = new ArrayList<>();
    private long lastSyncedAt;

    public DriveAccess() {
    }

    public DriveAccess(String encryptedEmail, String emailHash) {
        this.encryptedEmail = encryptedEmail;
        this.emailHash = emailHash;
    }
}
