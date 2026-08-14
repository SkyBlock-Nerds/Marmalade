package net.hypixel.nerdbot.marmalade.storage.database.model.user.drive;

/**
 * One Drive permission the bot created for this user. The Drive permission id
 * is stored so revocation is a direct delete, with no need to decrypt the
 * user's email and search the folder's permission list. accessLevel holds a
 * {@code DriveAccessLevel} enum name (READER/COMMENTER/WRITER).
 */
public record DriveGrant(String folderId, String permissionId, String accessLevel) {
}
