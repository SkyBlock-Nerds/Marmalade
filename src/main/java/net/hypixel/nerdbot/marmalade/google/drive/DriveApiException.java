package net.hypixel.nerdbot.marmalade.google.drive;

import lombok.Getter;
import net.hypixel.nerdbot.marmalade.exception.FormattedException;

/**
 * A Drive API call failed with a definitive (non-retryable) error, e.g. an
 * invalid email address or a missing folder. Retryable failures are the
 * {@link TransientDriveApiException} subclass.
 */
public class DriveApiException extends FormattedException {

    @Getter
    private final int statusCode;

    /**
     * Drive's machine-readable reason code (e.g. sharingRateLimitExceeded,
     * invalidSharingRequest), or "unknown" when the error body carried none.
     * Never contains user data, unlike the human-readable error message.
     */
    @Getter
    private String reason = "unknown";

    public DriveApiException(int statusCode, String message, Object... args) {
        super(message, args);
        this.statusCode = statusCode;
    }

    public DriveApiException(int statusCode, String message, Throwable cause, Object... args) {
        super(message, cause, args);
        this.statusCode = statusCode;
    }

    /** Fluent setter used at the HTTP layer where the error body is in hand. */
    public DriveApiException reason(String reason) {
        this.reason = reason == null || reason.isBlank() ? "unknown" : reason;
        return this;
    }
}
