package net.hypixel.nerdbot.marmalade.http;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import net.hypixel.nerdbot.marmalade.exception.HttpException;
import net.hypixel.nerdbot.marmalade.functional.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private void addHandler(String path, int status, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }

    // --- getString ---

    @Test
    void getString_returns_success_on_200() {
        addHandler("/ok", 200, "hello world");

        Result<String, HttpException> result = HttpClient.getString(url("/ok"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.orElse("")).isEqualTo("hello world");
    }

    @Test
    void getString_returns_failure_on_404() {
        addHandler("/missing", 404, "not found");

        Result<String, HttpException> result = HttpClient.getString(url("/missing"));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void getString_failure_carries_status_code_on_non_2xx() {
        addHandler("/error", 500, "server error");

        Result<String, HttpException> result = HttpClient.getString(url("/error"));

        assertThat(result.isFailure()).isTrue();
        Result.Failure<String, HttpException> failure = (Result.Failure<String, HttpException>) result;
        assertThat(failure.error().getStatusCode()).isEqualTo(500);
    }

    @Test
    void getString_failure_carries_url_on_non_2xx() {
        addHandler("/error", 503, "unavailable");
        String target = url("/error");

        Result<String, HttpException> result = HttpClient.getString(target);

        assertThat(result.isFailure()).isTrue();
        Result.Failure<String, HttpException> failure = (Result.Failure<String, HttpException>) result;
        assertThat(failure.error().getUrl()).isEqualTo(target);
    }

    @Test
    void getString_returns_failure_on_connection_refused() {
        // Port 1 is reserved and should always refuse connections on any OS
        Result<String, HttpException> result = HttpClient.getString("http://localhost:1/unreachable");

        assertThat(result.isFailure()).isTrue();
    }

    // --- getStringAsync ---

    @Test
    void getStringAsync_returns_success_on_200() throws Exception {
        addHandler("/async-ok", 200, "async body");

        CompletableFuture<Result<String, HttpException>> future = HttpClient.getStringAsync(url("/async-ok"));
        Result<String, HttpException> result = future.get();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.orElse("")).isEqualTo("async body");
    }

    @Test
    void getStringAsync_returns_failure_on_non_2xx() throws Exception {
        addHandler("/async-fail", 403, "forbidden");

        CompletableFuture<Result<String, HttpException>> future = HttpClient.getStringAsync(url("/async-fail"));
        Result<String, HttpException> result = future.get();

        assertThat(result.isFailure()).isTrue();
        Result.Failure<String, HttpException> failure = (Result.Failure<String, HttpException>) result;
        assertThat(failure.error().getStatusCode()).isEqualTo(403);
    }

    @Test
    void getStringAsync_returns_failure_on_connection_refused() throws Exception {
        CompletableFuture<Result<String, HttpException>> future = HttpClient.getStringAsync("http://localhost:1/unreachable");
        Result<String, HttpException> result = future.get();

        assertThat(result.isFailure()).isTrue();
    }

    // --- getJson ---

    @Test
    void getJson_returns_success_with_parsed_object() {
        addHandler("/json", 200, "{\"key\":\"value\"}");

        Result<JsonObject, HttpException> result = HttpClient.getJson(url("/json"));

        assertThat(result.isSuccess()).isTrue();
        JsonObject json = result.orElse(new JsonObject());
        assertThat(json.get("key").getAsString()).isEqualTo("value");
    }

    @Test
    void getJson_returns_failure_on_non_2xx() {
        addHandler("/json-error", 400, "{\"error\":\"bad request\"}");

        Result<JsonObject, HttpException> result = HttpClient.getJson(url("/json-error"));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void getJson_returns_failure_when_response_is_not_a_json_object() {
        addHandler("/json-array", 200, "[1,2,3]");

        Result<JsonObject, HttpException> result = HttpClient.getJson(url("/json-array"));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void getJson_returns_failure_when_response_is_invalid_json() {
        addHandler("/json-bad", 200, "not json at all");

        Result<JsonObject, HttpException> result = HttpClient.getJson(url("/json-bad"));

        assertThat(result.isFailure()).isTrue();
    }

    // --- getJsonAsync ---

    @Test
    void getJsonAsync_returns_success_with_parsed_object() throws Exception {
        addHandler("/async-json", 200, "{\"hello\":\"world\"}");

        CompletableFuture<Result<JsonObject, HttpException>> future = HttpClient.getJsonAsync(url("/async-json"));
        Result<JsonObject, HttpException> result = future.get();

        assertThat(result.isSuccess()).isTrue();
        JsonObject json = result.orElse(new JsonObject());
        assertThat(json.get("hello").getAsString()).isEqualTo("world");
    }

    @Test
    void getJsonAsync_returns_failure_on_non_2xx() throws Exception {
        addHandler("/async-json-error", 502, "bad gateway");

        CompletableFuture<Result<JsonObject, HttpException>> future = HttpClient.getJsonAsync(url("/async-json-error"));
        Result<JsonObject, HttpException> result = future.get();

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void getJsonAsync_returns_failure_when_body_is_not_a_json_object() throws Exception {
        addHandler("/async-json-array", 200, "[\"a\",\"b\"]");

        CompletableFuture<Result<JsonObject, HttpException>> future = HttpClient.getJsonAsync(url("/async-json-array"));
        Result<JsonObject, HttpException> result = future.get();

        assertThat(result.isFailure()).isTrue();
    }
}
