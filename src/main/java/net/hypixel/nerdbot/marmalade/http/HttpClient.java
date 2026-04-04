package net.hypixel.nerdbot.marmalade.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;
import net.hypixel.nerdbot.marmalade.exception.HttpException;
import net.hypixel.nerdbot.marmalade.functional.Result;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@UtilityClass
public class HttpClient {

    private static final Gson GSON = new GsonBuilder().create();

    private static final java.net.http.HttpClient CLIENT = java.net.http.HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
        .build();

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
                return Result.failure(new HttpException(
                    "HTTP request failed with status " + response.statusCode() + " for URL: " + url,
                    response.statusCode(),
                    url
                ));
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
                    return Result.failure(new HttpException(
                        "HTTP request failed with status " + response.statusCode() + " for URL: " + url,
                        response.statusCode(),
                        url
                    ));
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
