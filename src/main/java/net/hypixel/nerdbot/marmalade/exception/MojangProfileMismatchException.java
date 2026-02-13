package net.hypixel.nerdbot.marmalade.exception;

public class MojangProfileMismatchException extends MojangProfileException {

    public MojangProfileMismatchException(String message) {
        super(message);
    }

    public MojangProfileMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}