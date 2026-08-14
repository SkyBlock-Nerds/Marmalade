package net.hypixel.nerdbot.marmalade.google;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleTokenProviderTest {

    private static final String SCOPE = "https://www.googleapis.com/auth/drive";

    private KeyPair keyPair;
    private ServiceAccountKey key;
    private List<String> postedBodies;
    private GoogleTokenProvider.TokenHttp okHttp;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = ServiceAccountKeyTest.generateKeyPair();
        key = ServiceAccountKey.fromJson(ServiceAccountKeyTest.keyJson(keyPair));
        postedBodies = new ArrayList<>();
        okHttp = (url, body) -> {
            postedBodies.add(body);
            return new GoogleTokenProvider.TokenHttp.TokenResponse(200,
                "{\"access_token\":\"token-" + postedBodies.size() + "\",\"expires_in\":3600}");
        };
    }

    private static Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    @Test
    void exchangesSignedJwtForToken() throws Exception {
        Instant now = Instant.parse("2026-08-14T12:00:00Z");
        GoogleTokenProvider provider = new GoogleTokenProvider(key, SCOPE, okHttp, fixedClock(now));

        assertThat(provider.getAccessToken()).isEqualTo("token-1");

        String body = postedBodies.getFirst();
        assertThat(body).startsWith("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=");
        String assertion = URLDecoder.decode(body.substring(body.indexOf("assertion=") + "assertion=".length()), StandardCharsets.UTF_8);
        String[] segments = assertion.split("\\.");
        assertThat(segments).hasSize(3);

        JsonObject header = JsonParser.parseString(decodeSegment(segments[0])).getAsJsonObject();
        assertThat(header.get("alg").getAsString()).isEqualTo("RS256");
        assertThat(header.get("typ").getAsString()).isEqualTo("JWT");

        JsonObject claims = JsonParser.parseString(decodeSegment(segments[1])).getAsJsonObject();
        assertThat(claims.get("iss").getAsString()).isEqualTo("bot@project.iam.gserviceaccount.com");
        assertThat(claims.get("scope").getAsString()).isEqualTo(SCOPE);
        assertThat(claims.get("aud").getAsString()).isEqualTo("https://oauth2.googleapis.com/token");
        assertThat(claims.get("iat").getAsLong()).isEqualTo(now.getEpochSecond());
        assertThat(claims.get("exp").getAsLong()).isEqualTo(now.getEpochSecond() + 3600);

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keyPair.getPublic());
        verifier.update((segments[0] + "." + segments[1]).getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(Base64.getUrlDecoder().decode(segments[2]))).isTrue();
    }

    private static String decodeSegment(String segment) {
        return new String(Base64.getUrlDecoder().decode(segment), StandardCharsets.UTF_8);
    }

    @Test
    void cachesTokenUntilNearExpiry() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-14T12:00:00Z"));
        GoogleTokenProvider provider = new GoogleTokenProvider(key, SCOPE, okHttp, clock);

        provider.getAccessToken();
        provider.getAccessToken();
        assertThat(postedBodies).hasSize(1);

        clock.advance(Duration.ofMinutes(30));
        provider.getAccessToken();
        assertThat(postedBodies).hasSize(1);

        // +60m total: past the 3600s−60s refresh threshold, so a new token is minted
        clock.advance(Duration.ofMinutes(30));
        provider.getAccessToken();
        assertThat(postedBodies).hasSize(2);
    }

    @Test
    void surfacesHttpFailureAsAuthException() {
        GoogleTokenProvider.TokenHttp failing = (url, body) ->
            new GoogleTokenProvider.TokenHttp.TokenResponse(500, "boom");
        GoogleTokenProvider provider = new GoogleTokenProvider(key, SCOPE, failing, Clock.systemUTC());
        assertThatThrownBy(provider::getAccessToken).isInstanceOf(GoogleAuthException.class);
    }

    @Test
    void surfacesMalformedTokenResponseAsAuthException() {
        GoogleTokenProvider.TokenHttp weird = (url, body) ->
            new GoogleTokenProvider.TokenHttp.TokenResponse(200, "{\"unexpected\":true}");
        GoogleTokenProvider provider = new GoogleTokenProvider(key, SCOPE, weird, Clock.systemUTC());
        assertThatThrownBy(provider::getAccessToken).isInstanceOf(GoogleAuthException.class);
    }

    /** Minimal mutable Clock for cache-expiry tests. */
    static final class MutableClock extends Clock {
        private Instant instant;
        MutableClock(Instant start) { this.instant = start; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
