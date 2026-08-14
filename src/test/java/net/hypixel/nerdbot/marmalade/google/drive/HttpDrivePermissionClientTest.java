package net.hypixel.nerdbot.marmalade.google.drive;

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
    private final HttpDrivePermissionClient client = new HttpDrivePermissionClient(() -> "test-token", executor);

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
    void grantSurfacesClientErrorWithStatus() {
        executor.enqueue(400, "{\"error\":{\"message\":\"invalid sharing request\"}}");
        assertThatThrownBy(() -> client.grantPermission("folder-1", "nobody@example.com", DriveAccessLevel.READER))
            .isInstanceOf(DriveApiException.class)
            .isNotInstanceOf(TransientDriveApiException.class)
            .satisfies(e -> assertThat(((DriveApiException) e).getStatusCode()).isEqualTo(400));
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
        HttpDrivePermissionClient brokenClient = new HttpDrivePermissionClient(() -> "t", broken);
        assertThatThrownBy(() -> brokenClient.listPermissions("f"))
            .isInstanceOf(TransientDriveApiException.class);
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
