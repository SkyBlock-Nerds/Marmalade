package net.hypixel.nerdbot.marmalade.google.drive;

import net.hypixel.nerdbot.marmalade.google.GoogleAuthException;
import net.hypixel.nerdbot.marmalade.google.drive.HttpDrivePermissionClient.HttpExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpDrivePermissionClientTest {

    /** Scripted fake: replays queued responses and records every request. */
    static final class ScriptedExecutor implements HttpExecutor {
        final List<Request> requests = new ArrayList<>();
        final Deque<Response> responses = new ArrayDeque<>();

        void enqueue(int status, String body) {
            responses.add(new Response(status, body));
        }

        @Override
        public Response send(Request request) {
            requests.add(request);
            if (responses.isEmpty()) {
                throw new IllegalStateException("No scripted response for " + request.method() + " " + request.url());
            }
            return responses.poll();
        }
    }

    private final ScriptedExecutor executor = new ScriptedExecutor();
    private final HttpDrivePermissionClient client = new HttpDrivePermissionClient(executor);

    @Test
    void grantPostsPermissionAndReturnsId() throws DriveApiException {
        executor.enqueue(200, "{\"id\":\"perm-1\"}");

        String permissionId = client.grantPermission("folder-1", "user@example.com", DriveAccessLevel.WRITER);

        assertThat(permissionId).isEqualTo("perm-1");
        HttpExecutor.Request request = executor.requests.getFirst();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.url()).isEqualTo(
            "https://www.googleapis.com/drive/v3/files/folder-1/permissions?supportsAllDrives=true&sendNotificationEmail=false");
        assertThat(request.jsonBody()).contains("\"type\":\"user\"");
        assertThat(request.jsonBody()).contains("\"role\":\"writer\"");
        assertThat(request.jsonBody()).contains("\"emailAddress\":\"user@example.com\"");
    }

    @Test
    void grantCanRequestNotificationEmail() throws DriveApiException {
        ScriptedExecutor notifyingExecutor = new ScriptedExecutor();
        HttpDrivePermissionClient notifyingClient = new HttpDrivePermissionClient(notifyingExecutor, true);
        notifyingExecutor.enqueue(200, "{\"id\":\"perm-1\"}");

        notifyingClient.grantPermission("folder-1", "user@example.com", DriveAccessLevel.READER);

        assertThat(notifyingExecutor.requests.getFirst().url()).isEqualTo(
            "https://www.googleapis.com/drive/v3/files/folder-1/permissions?supportsAllDrives=true&sendNotificationEmail=true");
    }

    @Test
    void getFileNameFetchesMetadata() throws DriveApiException {
        executor.enqueue(200, "{\"name\":\"Members (Editor)\"}");
        assertThat(client.getFileName("folder-1")).isEqualTo("Members (Editor)");
        assertThat(executor.requests.getFirst().method()).isEqualTo("GET");
        assertThat(executor.requests.getFirst().url()).isEqualTo(
            "https://www.googleapis.com/drive/v3/files/folder-1?supportsAllDrives=true&fields=name");

        executor.enqueue(404, "not found");
        assertThatThrownBy(() -> client.getFileName("gone"))
            .isInstanceOf(DriveApiException.class)
            .isNotInstanceOf(TransientDriveApiException.class);
    }

    @Test
    void grantIncludesCustomEmailMessageOnlyWhenNotifying() throws DriveApiException {
        ScriptedExecutor notifyingExecutor = new ScriptedExecutor();
        HttpDrivePermissionClient notifyingClient = new HttpDrivePermissionClient(notifyingExecutor, true, "Shared by the bot");
        notifyingExecutor.enqueue(200, "{\"id\":\"perm-1\"}");
        notifyingClient.grantPermission("folder-1", "user@example.com", DriveAccessLevel.READER);
        assertThat(notifyingExecutor.requests.getFirst().url())
            .contains("sendNotificationEmail=true")
            .contains("&emailMessage=Shared%20by%20the%20bot");

        // Message is ignored while notifications are off
        ScriptedExecutor silentExecutor = new ScriptedExecutor();
        HttpDrivePermissionClient silentClient = new HttpDrivePermissionClient(silentExecutor, false, "Shared by the bot");
        silentExecutor.enqueue(200, "{\"id\":\"perm-2\"}");
        silentClient.grantPermission("folder-1", "user@example.com", DriveAccessLevel.READER);
        assertThat(silentExecutor.requests.getFirst().url()).doesNotContain("emailMessage");
    }

    @Test
    void grantSurfacesClientErrorWithStatus() {
        executor.enqueue(400, "{\"error\":{\"message\":\"invalid sharing request\"}}");
        assertThatThrownBy(() -> client.grantPermission("folder-1", "nobody@example.com", DriveAccessLevel.READER))
            .isInstanceOf(DriveApiException.class)
            .isNotInstanceOf(TransientDriveApiException.class)
            .satisfies(e -> assertThat(((DriveApiException) e).getStatusCode()).isEqualTo(400));
    }

    @Test
    void errorReasonCodeIsExtractedButMessageIsNot() {
        executor.enqueue(403, """
            {"error":{"code":403,"message":"Rate limit exceeded sharing to nobody@example.com.",
            "errors":[{"reason":"sharingRateLimitExceeded","message":"Rate limit exceeded sharing to nobody@example.com."}]}}
            """);
        assertThatThrownBy(() -> client.grantPermission("folder-1", "nobody@example.com", DriveAccessLevel.READER))
            .isInstanceOf(DriveApiException.class)
            .satisfies(e -> {
                DriveApiException drive = (DriveApiException) e;
                assertThat(drive.getReason()).isEqualTo("sharingRateLimitExceeded");
                assertThat(drive.getMessage()).contains("sharingRateLimitExceeded").doesNotContain("nobody@example.com");
            });

        // Unparseable body degrades to "unknown"
        executor.enqueue(400, "not json");
        assertThatThrownBy(() -> client.listPermissions("folder-1"))
            .isInstanceOf(DriveApiException.class)
            .satisfies(e -> assertThat(((DriveApiException) e).getReason()).isEqualTo("unknown"));
    }

    @Test
    void rateLimitAndServerErrorsAreTransient() {
        executor.enqueue(429, "slow down");
        assertThatThrownBy(() -> client.grantPermission("f", "e@x.com", DriveAccessLevel.READER))
            .isInstanceOf(TransientDriveApiException.class);

        executor.enqueue(503, "unavailable");
        assertThatThrownBy(() -> client.listPermissions("f"))
            .isInstanceOf(TransientDriveApiException.class);
    }

    @Test
    void networkFailureIsTransient() {
        HttpExecutor broken = request -> {
            throw new java.io.IOException("connection reset");
        };
        HttpDrivePermissionClient brokenClient = new HttpDrivePermissionClient(broken);
        assertThatThrownBy(() -> brokenClient.listPermissions("f"))
            .isInstanceOf(TransientDriveApiException.class);
    }

    @Test
    void authFailureIsNotTransient() {
        HttpExecutor authFailed = request -> {
            throw new GoogleAuthException("unable to obtain access token");
        };
        HttpDrivePermissionClient brokenClient = new HttpDrivePermissionClient(authFailed);
        assertThatThrownBy(() -> brokenClient.listPermissions("f"))
            .isInstanceOf(DriveApiException.class)
            .isNotInstanceOf(TransientDriveApiException.class);
    }

    @Test
    void revokeDeletesAndTreatsNotFoundAsSuccess() throws DriveApiException {
        executor.enqueue(204, "");
        client.revokePermission("folder-1", "perm-1");
        assertThat(executor.requests.getFirst().method()).isEqualTo("DELETE");
        assertThat(executor.requests.getFirst().url()).isEqualTo(
            "https://www.googleapis.com/drive/v3/files/folder-1/permissions/perm-1?supportsAllDrives=true");

        // Already-deleted permission: desired state reached, no exception
        executor.enqueue(404, "not found");
        client.revokePermission("folder-1", "perm-gone");
    }

    @Test
    void listFollowsPagination() throws DriveApiException {
        executor.enqueue(200, """
            {"permissions":[{"id":"p1","emailAddress":"a@x.com","role":"reader"}],"nextPageToken":"page2"}
            """);
        executor.enqueue(200, """
            {"permissions":[{"id":"p2","emailAddress":"b@x.com","role":"writer"}]}
            """);

        List<DrivePermission> permissions = client.listPermissions("folder-1");

        assertThat(permissions).containsExactly(
            new DrivePermission("p1", "a@x.com", "reader"),
            new DrivePermission("p2", "b@x.com", "writer"));
        assertThat(executor.requests).hasSize(2);
        assertThat(executor.requests.get(1).url()).contains("pageToken=page2");
    }

    @Test
    void urlEncodesIds() throws DriveApiException {
        executor.enqueue(200, "{\"id\":\"p\"}");
        client.grantPermission("folder with spaces", "e@x.com", DriveAccessLevel.READER);
        assertThat(executor.requests.getFirst().url()).contains("folder%20with%20spaces");
    }
}
