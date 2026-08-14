package net.hypixel.nerdbot.marmalade.google;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceAccountKeyTest {

    static String pemFor(KeyPair keyPair) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
            .encodeToString(keyPair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    }

    static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    static String keyJson(KeyPair keyPair) {
        // Real SA key files escape newlines in the PEM; replicate that
        String escapedPem = pemFor(keyPair).replace("\n", "\\n");
        return """
            {
              "type": "service_account",
              "client_email": "bot@project.iam.gserviceaccount.com",
              "token_uri": "https://oauth2.googleapis.com/token",
              "private_key": "%s"
            }
            """.formatted(escapedPem);
    }

    @Test
    void parsesRealShapedKeyFile() throws Exception {
        KeyPair keyPair = generateKeyPair();
        ServiceAccountKey key = ServiceAccountKey.fromJson(keyJson(keyPair));
        assertThat(key.clientEmail()).isEqualTo("bot@project.iam.gserviceaccount.com");
        assertThat(key.tokenUri()).isEqualTo("https://oauth2.googleapis.com/token");
        assertThat(key.privateKey().getEncoded()).isEqualTo(keyPair.getPrivate().getEncoded());
    }

    @Test
    void rejectsJsonMissingFields() {
        assertThatThrownBy(() -> ServiceAccountKey.fromJson("{\"type\":\"service_account\"}"))
            .isInstanceOf(GoogleAuthException.class);
    }

    @Test
    void rejectsMalformedPem() {
        String json = """
            {"client_email":"a@b.c","token_uri":"https://t","private_key":"-----BEGIN PRIVATE KEY-----\\nnot-a-key\\n-----END PRIVATE KEY-----\\n"}
            """;
        assertThatThrownBy(() -> ServiceAccountKey.fromJson(json)).isInstanceOf(GoogleAuthException.class);
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> ServiceAccountKey.fromJson("not json")).isInstanceOf(GoogleAuthException.class);
    }

    @Test
    void toStringRedactsPrivateKey() throws Exception {
        KeyPair keyPair = generateKeyPair();
        ServiceAccountKey key = ServiceAccountKey.fromJson(keyJson(keyPair));
        String toString = key.toString();

        // Must include public-facing fields
        assertThat(toString).contains("bot@project.iam.gserviceaccount.com");

        // Must redact the key
        assertThat(toString).contains("<redacted>");

        // Must not contain private key markers or material
        assertThat(toString).doesNotContain("BEGIN PRIVATE");
        assertThat(toString).doesNotContain(String.valueOf(key.privateKey().getPrivateExponent()));
    }
}
