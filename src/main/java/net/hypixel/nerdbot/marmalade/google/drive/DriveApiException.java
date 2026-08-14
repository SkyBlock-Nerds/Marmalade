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

    public DriveApiException(int statusCode, String message, Object... args) {
        super(message, args);
        this.statusCode = statusCode;
    }

    public DriveApiException(int statusCode, String message, Throwable cause, Object... args) {
        super(message, cause, args);
        this.statusCode = statusCode;
    }
}
