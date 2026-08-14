package net.hypixel.nerdbot.marmalade.google.drive;

/**
 * A single entry from a folder's permission list, as returned by the Drive v3
 * permissions API. {@code role} is the raw API string because folders can carry
 * roles this system does not manage (e.g. organizer).
 */
public record DrivePermission(String id, String emailAddress, String role) {
}
