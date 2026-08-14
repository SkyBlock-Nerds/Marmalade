package net.hypixel.nerdbot.marmalade.google.drive;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.hypixel.nerdbot.marmalade.google.AccessTokenProvider;
import net.hypixel.nerdbot.marmalade.google.GoogleAuthException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Drive v3 permissions over plain HTTP. All calls pass supportsAllDrives=true
 * (the folders live in a shared drive) and grants suppress Google's
 * notification email — members are told about their access in Discord instead.
 */
public class HttpDrivePermissionClient implements DrivePermissionClient {

    private static final String BASE_URL = "https://www.googleapis.com/drive/v3/files/";

    private final AccessTokenProvider tokenProvider;
    private final HttpExecutor executor;

    /** HTTP seam so tests can script responses without a network. */
    public interface HttpExecutor {

        Response send(Request request) throws Exception;

        record Request(String method, String url, String jsonBody) {
        }

        record Response(int statusCode, String body) {
        }
    }

    public HttpDrivePermissionClient(AccessTokenProvider tokenProvider, HttpExecutor executor) {
        this.tokenProvider = tokenProvider;
        this.executor = executor;
    }

    public static HttpDrivePermissionClient createDefault(AccessTokenProvider tokenProvider) {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        return new HttpDrivePermissionClient(tokenProvider, request -> {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(request.url()))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + tokenProvider.getAccessToken());

            if (request.jsonBody() != null) {
                builder.header("Content-Type", "application/json")
                    .method(request.method(), HttpRequest.BodyPublishers.ofString(request.jsonBody()));
            } else {
                builder.method(request.method(), HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new HttpExecutor.Response(response.statusCode(), response.body());
        });
    }

    @Override
    public String grantPermission(String folderId, String email, DriveAccessLevel level) throws DriveApiException {
        JsonObject body = new JsonObject();
        body.addProperty("type", "user");
        body.addProperty("role", level.getApiRole());
        body.addProperty("emailAddress", email);

        String url = BASE_URL + encode(folderId) + "/permissions?supportsAllDrives=true&sendNotificationEmail=false";
        HttpExecutor.Response response = send(new HttpExecutor.Request("POST", url, body.toString()));
        requireSuccess(response, "grant permission on folder {}", folderId);

        JsonObject parsed = parseObject(response.body(), folderId);
        if (!parsed.has("id")) {
            throw new DriveApiException(response.statusCode(), "Grant response for folder {} had no permission id", folderId);
        }
        return parsed.get("id").getAsString();
    }

    @Override
    public void revokePermission(String folderId, String permissionId) throws DriveApiException {
        String url = BASE_URL + encode(folderId) + "/permissions/" + encode(permissionId) + "?supportsAllDrives=true";
        HttpExecutor.Response response = send(new HttpExecutor.Request("DELETE", url, null));

        if (response.statusCode() == 404) {
            return; // already gone — the state we wanted
        }
        requireSuccess(response, "revoke permission {} on folder {}", permissionId, folderId);
    }

    @Override
    public List<DrivePermission> listPermissions(String folderId) throws DriveApiException {
        List<DrivePermission> permissions = new ArrayList<>();
        String pageToken = null;

        do {
            String url = BASE_URL + encode(folderId)
                + "/permissions?supportsAllDrives=true&fields=nextPageToken,permissions(id,emailAddress,role)"
                + (pageToken != null ? "&pageToken=" + encode(pageToken) : "");
            HttpExecutor.Response response = send(new HttpExecutor.Request("GET", url, null));
            requireSuccess(response, "list permissions on folder {}", folderId);

            JsonObject parsed = parseObject(response.body(), folderId);
            JsonArray page = parsed.has("permissions") ? parsed.getAsJsonArray("permissions") : new JsonArray();
            for (JsonElement element : page) {
                JsonObject permission = element.getAsJsonObject();
                permissions.add(new DrivePermission(
                    permission.has("id") ? permission.get("id").getAsString() : null,
                    permission.has("emailAddress") ? permission.get("emailAddress").getAsString() : null,
                    permission.has("role") ? permission.get("role").getAsString() : null
                ));
            }
            pageToken = parsed.has("nextPageToken") ? parsed.get("nextPageToken").getAsString() : null;
        } while (pageToken != null);

        return permissions;
    }

    private HttpExecutor.Response send(HttpExecutor.Request request) throws DriveApiException {
        try {
            return executor.send(request);
        } catch (GoogleAuthException e) {
            throw new DriveApiException(-1, "Could not obtain an access token", e);
        } catch (Exception e) {
            throw new TransientDriveApiException(-1, "Drive request failed at the network level", e);
        }
    }

    private static void requireSuccess(HttpExecutor.Response response, String action, Object... args) throws DriveApiException {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        if (status == 429 || status >= 500) {
            throw new TransientDriveApiException(status, "Transient Drive failure (HTTP {}) trying to " + action, prepend(status, args));
        }
        throw new DriveApiException(status, "Drive rejected request (HTTP {}) trying to " + action, prepend(status, args));
    }

    private static Object[] prepend(Object first, Object[] rest) {
        Object[] combined = new Object[rest.length + 1];
        combined[0] = first;
        System.arraycopy(rest, 0, combined, 1, rest.length);
        return combined;
    }

    private static JsonObject parseObject(String body, String folderId) throws DriveApiException {
        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new DriveApiException(-1, "Drive returned unparseable JSON for folder {}", e, folderId);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
