package net.hypixel.nerdbot.marmalade.exception;

public class RepositoryException extends Exception {

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}