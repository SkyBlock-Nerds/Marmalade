package net.hypixel.nerdbot.marmalade.exception;

/**
 * A download refused because of what the server offered rather than a transport failure: a host
 * outside the caller's allowlist (including the final host after redirects) or a body larger than
 * the caller's cap. Retrying the same request fails the same way, so callers should not treat this
 * as transient.
 */
public class DownloadRejectedException extends HttpException {

    public DownloadRejectedException(String message, String url) {
        super(message, -1, url);
    }
}
