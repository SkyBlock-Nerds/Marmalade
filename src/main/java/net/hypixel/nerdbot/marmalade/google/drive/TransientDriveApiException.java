package net.hypixel.nerdbot.marmalade.google.drive;

/**
 * A Drive API failure worth retrying: rate limiting (429), server errors (5xx),
 * or a network-level failure (status code -1). Callers pair this with
 * {@code Retry.retryOn(TransientDriveApiException.class)}.
 */
public class TransientDriveApiException extends DriveApiException {

    public TransientDriveApiException(int statusCode, String message, Object... args) {
        super(statusCode, message, args);
    }

    public TransientDriveApiException(int statusCode, String message, Throwable cause, Object... args) {
        super(statusCode, message, cause, args);
    }
}
