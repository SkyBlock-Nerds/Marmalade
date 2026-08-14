package net.hypixel.nerdbot.marmalade.google.drive;

import java.util.List;

/**
 * The three Drive operations needed to manage per-user folder access. Kept as
 * an interface so consumers test against in-memory fakes.
 */
public interface DrivePermissionClient {

    /**
     * Grants {@code email} the given access level on a folder.
     *
     * @return the Drive permission id, which callers must store — revocation
     *         is by id, not by email
     */
    String grantPermission(String folderId, String email, DriveAccessLevel level) throws DriveApiException;

    /**
     * Deletes a permission by id. A permission that no longer exists (404) is
     * treated as success: the desired state is "gone" either way.
     */
    void revokePermission(String folderId, String permissionId) throws DriveApiException;

    /** Lists every permission on a folder, following pagination. */
    List<DrivePermission> listPermissions(String folderId) throws DriveApiException;
}
