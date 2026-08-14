package net.hypixel.nerdbot.marmalade.security;

import net.hypixel.nerdbot.marmalade.exception.FormattedException;

/**
 * Thrown when decryption fails: tampered or truncated ciphertext, a wrong key,
 * or input that is not valid base64.
 */
public class CipherException extends FormattedException {

    public CipherException(String message, Throwable cause, Object... args) {
        super(message, cause, args);
    }
}
