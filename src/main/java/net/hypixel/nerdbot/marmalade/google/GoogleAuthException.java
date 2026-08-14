package net.hypixel.nerdbot.marmalade.google;

import net.hypixel.nerdbot.marmalade.exception.FormattedException;

/**
 * Thrown when a Google service-account key cannot be parsed or an access-token
 * exchange fails.
 */
public class GoogleAuthException extends FormattedException {

    public GoogleAuthException(String message, Object... args) {
        super(message, args);
    }

    public GoogleAuthException(String message, Throwable cause, Object... args) {
        super(message, cause, args);
    }
}
