package net.hypixel.nerdbot.marmalade.exception;

/**
 * A {@link RuntimeException} whose message supports SLF4J-style {@code {}} placeholders,
 * substituted safely at construction time without throwing if arguments are missing.
 */
public class FormattedException extends RuntimeException {

    /**
     * Creates an exception with a formatted message; {@code {}} tokens are replaced by {@code args} in order.
     *
     * @param message the message template, using {@code {}} as the placeholder token
     * @param args    the arguments to substitute into the template
     */
    public FormattedException(String message, Object... args) {
        super(safeFormat(message, args));
    }

    /**
     * Creates an exception with a formatted message and a root cause.
     *
     * @param message the message template, using {@code {}} as the placeholder token
     * @param cause   the underlying throwable that triggered this exception
     * @param args    the arguments to substitute into the template
     */
    public FormattedException(String message, Throwable cause, Object... args) {
        super(safeFormat(message, args), cause);
    }

    /**
     * Creates an exception that wraps an existing {@link Throwable}, with no additional message.
     *
     * @param cause the underlying throwable to wrap
     */
    public FormattedException(Throwable cause) {
        super(cause);
    }

    private static String safeFormat(String message, Object... args) {
        if (message == null) {
            return null;
        }

        if (args == null || args.length == 0) {
            return message;
        }

        StringBuilder sb = new StringBuilder(message.length() + 32);
        int argIndex = 0;
        int i = 0;

        while (i < message.length()) {
            if (i + 1 < message.length() && message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(String.valueOf(args[argIndex++]));
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(message.charAt(i));
                i++;
            }
        }

        return sb.toString();
    }
}
