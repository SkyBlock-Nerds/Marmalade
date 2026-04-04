package net.hypixel.nerdbot.marmalade.exception;

public class HttpException extends RuntimeException {

    private final int statusCode;
    private final String url;

    public HttpException(String message) {
        super(message);
        this.statusCode = -1;
        this.url = null;
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.url = null;
    }

    public HttpException(String message, int statusCode, String url) {
        super(message);
        this.statusCode = statusCode;
        this.url = url;
    }

    public HttpException(String message, Throwable cause, int statusCode, String url) {
        super(message, cause);
        this.statusCode = statusCode;
        this.url = url;
    }

    /**
     * Returns the HTTP status code, or -1 if not applicable.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the URL that caused the error, or null if not applicable.
     */
    public String getUrl() {
        return url;
    }
}
