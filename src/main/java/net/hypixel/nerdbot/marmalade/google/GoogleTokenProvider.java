package net.hypixel.nerdbot.marmalade.google;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Mints Google OAuth2 access tokens from a service-account key using the
 * JWT-bearer flow (RFC 7523): build a JWT, sign it RS256 with the account's
 * private key, and exchange it at the token endpoint. Tokens are cached and
 * refreshed 60 seconds before expiry. No Google SDK involved by design — the
 * flow is three well-specified steps and this library keeps its dependency
 * list minimal.
 */
public final class GoogleTokenProvider implements AccessTokenProvider {

    /** Full Drive access; required to manage permissions on shared-drive folders. */
    public static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);
    private static final Duration REFRESH_MARGIN = Duration.ofSeconds(60);

    private final ServiceAccountKey key;
    private final String scope;
    private final TokenHttp http;
    private final Clock clock;

    private String cachedToken;
    private Instant cachedTokenExpiry = Instant.EPOCH;

    /** HTTP seam so tests can capture the JWT without a network. */
    public interface TokenHttp {

        TokenResponse post(String url, String formBody) throws Exception;

        record TokenResponse(int statusCode, String body) {
        }
    }

    public GoogleTokenProvider(ServiceAccountKey key) {
        this(key, DRIVE_SCOPE, defaultHttp(), Clock.systemUTC());
    }

    public GoogleTokenProvider(ServiceAccountKey key, String scope, TokenHttp http, Clock clock) {
        this.key = key;
        this.scope = scope;
        this.http = http;
        this.clock = clock;
    }

    @Override
    public synchronized String getAccessToken() throws GoogleAuthException {
        Instant now = clock.instant();
        if (cachedToken != null && now.isBefore(cachedTokenExpiry.minus(REFRESH_MARGIN))) {
            return cachedToken;
        }

        String jwt = buildSignedJwt(now);
        String form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
            + "&assertion=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);

        TokenHttp.TokenResponse response;
        try {
            response = http.post(key.tokenUri(), form);
        } catch (Exception e) {
            throw new GoogleAuthException("Token exchange request failed", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new GoogleAuthException("Token exchange returned HTTP {}", response.statusCode());
        }

        try {
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!body.has("access_token") || !body.has("expires_in")) {
                throw new GoogleAuthException("Token response missing access_token or expires_in");
            }
            cachedToken = body.get("access_token").getAsString();
            cachedTokenExpiry = now.plusSeconds(body.get("expires_in").getAsLong());
            return cachedToken;
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new GoogleAuthException("Token response was not valid JSON", e);
        }
    }

    private String buildSignedJwt(Instant now) throws GoogleAuthException {
        String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");

        JsonObject claims = new JsonObject();
        claims.addProperty("iss", key.clientEmail());
        claims.addProperty("scope", scope);
        claims.addProperty("aud", key.tokenUri());
        claims.addProperty("iat", now.getEpochSecond());
        claims.addProperty("exp", now.plus(TOKEN_LIFETIME).getEpochSecond());
        String payload = base64Url(claims.toString());

        String signingInput = header + "." + payload;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key.privateKey());
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
            return signingInput + "." + encodedSignature;
        } catch (GeneralSecurityException e) {
            throw new GoogleAuthException("Failed to sign JWT with service account key", e);
        }
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static TokenHttp defaultHttp() {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        return (url, formBody) -> {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new TokenHttp.TokenResponse(response.statusCode(), response.body());
        };
    }
}
