package net.hypixel.nerdbot.marmalade.google;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * The parts of a Google service-account JSON key file needed to mint access
 * tokens: the account's email, the token endpoint, and its RSA private key
 * (PKCS#8 PEM in the file, parsed here into a usable key object).
 */
public record ServiceAccountKey(String clientEmail, String tokenUri, RSAPrivateKey privateKey) {

    public static ServiceAccountKey fromFile(Path path) throws GoogleAuthException {
        try {
            return fromJson(Files.readString(path));
        } catch (IOException e) {
            throw new GoogleAuthException("Failed to read service account key file {}", e, path);
        }
    }

    public static ServiceAccountKey fromJson(String json) throws GoogleAuthException {
        JsonObject object;
        try {
            object = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new GoogleAuthException("Service account key is not valid JSON", e);
        }

        if (!object.has("client_email") || !object.has("token_uri") || !object.has("private_key")) {
            throw new GoogleAuthException("Service account key is missing client_email, token_uri, or private_key");
        }

        return new ServiceAccountKey(
            object.get("client_email").getAsString(),
            object.get("token_uri").getAsString(),
            parsePrivateKey(object.get("private_key").getAsString())
        );
    }

    private static RSAPrivateKey parsePrivateKey(String pem) throws GoogleAuthException {
        String base64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new GoogleAuthException("Service account private_key is not a valid PKCS#8 RSA key", e);
        }
    }
}
