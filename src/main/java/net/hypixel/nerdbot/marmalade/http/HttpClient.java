package net.hypixel.nerdbot.marmalade.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;
import net.hypixel.nerdbot.marmalade.concurrent.ExecutorFactory;
import net.hypixel.nerdbot.marmalade.exception.DownloadRejectedException;
import net.hypixel.nerdbot.marmalade.exception.HttpException;
import net.hypixel.nerdbot.marmalade.functional.Result;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@UtilityClass
public class HttpClient {

    private static final class DownloadWatchdog {
        private static final ScheduledExecutorService EXECUTOR = ExecutorFactory.newScheduledDaemon("http-download-watchdog");
    }

    private static final Gson GSON = new GsonBuilder().create();

    private static final java.net.http.HttpClient CLIENT = java.net.http.HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
        .build();

    private static final int DOWNLOAD_BUFFER_BYTES = 64 * 1024;
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(2);

    /**
     * Streams an HTTPS GET response body into {@code target}, computing its SHA-1 on the way, so
     * large bodies are never held in memory. The request host and the final host after redirects
     * must both be in {@code allowedHosts}, and the scheme must be https. {@code allowedHosts} is
     * normalised here (trimmed, lowercased, blanks dropped), so callers need not pre-normalise it.
     * The body is capped at {@code maxBytes}: a declared Content-Length over the cap fails before
     * reading, and a body without one fails as soon as it passes the cap.
     *
     * <p>The entire download, including headers and body, must complete within 2 minutes. Hitting
     * this deadline fails with a plain {@link HttpException}.
     *
     * <p>A {@code maxBytes} that is zero or negative is a programming error and throws
     * {@link IllegalArgumentException} before any request is made.
     *
     * <p>Host and size refusals fail with {@link DownloadRejectedException}; network errors,
     * interruption and non-2xx statuses fail with a plain {@link HttpException}. On any failure
     * the partial {@code target} is deleted. Note that a redirect to a disallowed host has already
     * been requested by the time it is refused; only its body is never read.
     *
     * @param uri          The URI to download
     * @param target       The file to write; created or truncated
     * @param maxBytes     The largest body accepted
     * @param allowedHosts Host names the download may be served from; trimmed and lowercased, blanks ignored
     *
     * @return The download result, or the reason it failed
     *
     * @throws IllegalArgumentException if {@code maxBytes} is not positive
     */
    @NotNull
    public static Result<DownloadResult, HttpException> downloadToFile(@NotNull URI uri, @NotNull Path target,
                                                                       long maxBytes, @NotNull Set<String> allowedHosts) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive, got: " + maxBytes);
        }

        Set<String> normalisedHosts = allowedHosts.stream()
            .map(host -> host.trim().toLowerCase(Locale.ROOT))
            .filter(host -> !host.isEmpty())
            .collect(Collectors.toUnmodifiableSet());

        Result<DownloadResult, HttpException> result = streamToFile(uri, target, maxBytes, normalisedHosts);

        if (result.isFailure()) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // Best effort: a leftover partial file is overwritten by the next attempt
            }
        }

        return result;
    }

    private static Result<DownloadResult, HttpException> streamToFile(URI uri, Path target, long maxBytes, Set<String> allowedHosts) {
        String url = uri.toString();

        String refusal = hostRefusal(uri, allowedHosts, false);

        if (refusal != null) {
            return Result.failure(new DownloadRejectedException(refusal, url));
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(DOWNLOAD_TIMEOUT)
            .GET()
            .build();

        AtomicBoolean timedOut = new AtomicBoolean();
        ScheduledFuture<?> scheduledTask = null;

        try {
            long deadlineNanos = System.nanoTime() + DOWNLOAD_TIMEOUT.toNanos();
            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            InputStream body = response.body();
            long remainingNanos = deadlineNanos - System.nanoTime();

            if (remainingNanos <= 0) {
                closeQuietly(body);
                return Result.failure(timeoutFailure(url, null));
            }

            scheduledTask = DownloadWatchdog.EXECUTOR.schedule(
                () -> {
                    timedOut.set(true);
                    closeQuietly(body);
                },
                remainingNanos,
                TimeUnit.NANOSECONDS
            );

            try {
                URI finalUri = response.uri();

                String finalRefusal = hostRefusal(finalUri, allowedHosts, true);

                if (finalRefusal != null) {
                    return Result.failure(new DownloadRejectedException(finalRefusal, url));
                }

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    return Result.failure(statusFailure(response.statusCode(), url));
                }

                OptionalLong declaredLength = response.headers().firstValueAsLong("Content-Length");
                if (declaredLength.isPresent() && declaredLength.getAsLong() > maxBytes) {
                    return Result.failure(new DownloadRejectedException(
                        "Download declares " + declaredLength.getAsLong() + " bytes, over the " + maxBytes + " byte cap", url));
                }

                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                long written = 0;

                try (OutputStream out = Files.newOutputStream(target)) {
                    byte[] buffer = new byte[DOWNLOAD_BUFFER_BYTES];
                    int read;

                    while ((read = body.read(buffer)) != -1) {
                        written += read;

                        if (written > maxBytes) {
                            return Result.failure(new DownloadRejectedException(
                                "Download passed the " + maxBytes + " byte cap", url));
                        }

                        digest.update(buffer, 0, read);
                        out.write(buffer, 0, read);
                    }
                }

                return Result.success(new DownloadResult(target, written, HexFormat.of().formatHex(digest.digest()), finalUri));
            } finally {
                closeQuietly(body);
                if (scheduledTask != null) {
                    scheduledTask.cancel(false);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure(new HttpException("Download interrupted for URL: " + url, e, -1, url));
        } catch (IOException e) {
            if (timedOut.get()) {
                return Result.failure(timeoutFailure(url, e));
            }
            return Result.failure(new HttpException("Download failed for URL: " + url, e, -1, url));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available in this JVM", e);
        }
    }

    /**
     * Checks a URI against the https and host allowlist rules.
     *
     * @param redirected true when {@code uri} is the final URI after redirects, which changes the wording
     *
     * @return The refusal message, or null when the URI is allowed
     */
    private static String hostRefusal(URI uri, Set<String> allowedHosts, boolean redirected) {
        String prefix = redirected ? "Download was redirected to a URL that " : "Download ";

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return redirected
                ? prefix + "is not https, got: " + uri.getScheme()
                : prefix + "must use https, got: " + uri.getScheme();
        }

        String host = uri.getHost();

        if (host == null) {
            return prefix + (redirected ? "has no host: " : "URL has no host: ") + uri;
        }

        if (!allowedHosts.contains(host.toLowerCase(Locale.ROOT))) {
            return prefix + (redirected ? "has a host that is not allowed: " : "host is not allowed: ") + host;
        }

        return null;
    }

    private static HttpException timeoutFailure(String url, IOException cause) {
        String message = "Download did not finish within " + DOWNLOAD_TIMEOUT.toSeconds() + " seconds for URL: " + url;
        return cause == null ? new HttpException(message, -1, url) : new HttpException(message, cause, -1, url);
    }

    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // Best effort: closing only releases the connection
        }
    }

    private static HttpException statusFailure(int statusCode, String url) {
        return new HttpException("HTTP request failed with status " + statusCode + " for URL: " + url, statusCode, url);
    }

    /**
     * Makes a synchronous HTTP GET request and returns the response body as a string.
     * Returns Result.success with the body on HTTP 2xx, or Result.failure with an
     * HttpException on network error, interruption, or a non-2xx status code.
     *
     * @param url The URL to request
     * @return A Result containing the response body or an HttpException describing the failure
     */
    @NotNull
    public static Result<String, HttpException> getString(@NotNull String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Result.failure(statusFailure(response.statusCode(), url));
            }

            return Result.success(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure(new HttpException("HTTP request interrupted for URL: " + url, e, -1, url));
        } catch (IOException e) {
            return Result.failure(new HttpException("HTTP request failed for URL: " + url, e, -1, url));
        }
    }

    /**
     * Makes an asynchronous HTTP GET request and returns the response body as a string.
     * The future completes with Result.success on HTTP 2xx, or Result.failure on error.
     *
     * @param url The URL to request
     * @return A CompletableFuture that completes with a Result containing the body or an HttpException
     */
    @NotNull
    public static CompletableFuture<Result<String, HttpException>> getStringAsync(@NotNull String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .handle((response, throwable) -> {
                if (throwable != null) {
                    return Result.failure(new HttpException(
                        "HTTP request failed for URL: " + url,
                        throwable,
                        -1,
                        url
                    ));
                }

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    return Result.failure(statusFailure(response.statusCode(), url));
                }

                return Result.success(response.body());
            });
    }

    /**
     * Makes a synchronous HTTP GET request and parses the response as JSON.
     * Returns Result.success with the parsed JsonObject on HTTP 2xx, or Result.failure
     * with an HttpException on network error, non-2xx status, or invalid JSON.
     *
     * @param url The URL to request
     * @return A Result containing the parsed JsonObject or an HttpException describing the failure
     */
    @NotNull
    public static Result<JsonObject, HttpException> getJson(@NotNull String url) {
        Result<String, HttpException> stringResult = getString(url);

        if (stringResult.isFailure()) {
            return stringResult.map(ignored -> new JsonObject());
        }

        String body = stringResult.orElse("");

        try {
            JsonElement element = GSON.fromJson(body, JsonElement.class);

            if (element == null || !element.isJsonObject()) {
                return Result.failure(new HttpException(
                    "Response from URL " + url + " is not a JSON object",
                    -1,
                    url
                ));
            }

            return Result.success(element.getAsJsonObject());
        } catch (Exception e) {
            return Result.failure(new HttpException(
                "Failed to parse JSON response from URL: " + url,
                e,
                -1,
                url
            ));
        }
    }

    /**
     * Makes an asynchronous HTTP GET request and parses the response as JSON.
     * The future completes with Result.success on HTTP 2xx and valid JSON, or Result.failure on error.
     *
     * @param url The URL to request
     * @return A CompletableFuture that completes with a Result containing the JsonObject or an HttpException
     */
    @NotNull
    public static CompletableFuture<Result<JsonObject, HttpException>> getJsonAsync(@NotNull String url) {
        return getStringAsync(url)
            .thenApply(stringResult -> {
                if (stringResult.isFailure()) {
                    return stringResult.map(ignored -> new JsonObject());
                }

                String body = stringResult.orElse("");

                try {
                    JsonElement element = GSON.fromJson(body, JsonElement.class);

                    if (element == null || !element.isJsonObject()) {
                        return Result.failure(new HttpException(
                            "Response from URL " + url + " is not a JSON object",
                            -1,
                            url
                        ));
                    }

                    return Result.success(element.getAsJsonObject());
                } catch (Exception e) {
                    return Result.failure(new HttpException(
                        "Failed to parse JSON response from URL: " + url,
                        e,
                        -1,
                        url
                    ));
                }
            });
    }

    /**
     * Gets the underlying HttpClient instance for advanced usage.
     * Modules can use this for custom request building.
     *
     * @return The shared HttpClient instance
     */
    @NotNull
    public static java.net.http.HttpClient getClient() {
        return CLIENT;
    }
}
