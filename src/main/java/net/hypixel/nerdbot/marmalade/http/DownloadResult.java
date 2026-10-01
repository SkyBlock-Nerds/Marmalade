package net.hypixel.nerdbot.marmalade.http;

import java.net.URI;
import java.nio.file.Path;

/**
 * A completed download.
 *
 * @param file     The file the body was written to
 * @param bytes    Number of body bytes written
 * @param sha1Hex  Lowercase hex SHA-1 of the body, computed while streaming
 * @param finalUri The URI the body was served from after redirects
 */
public record DownloadResult(Path file, long bytes, String sha1Hex, URI finalUri) {
}
