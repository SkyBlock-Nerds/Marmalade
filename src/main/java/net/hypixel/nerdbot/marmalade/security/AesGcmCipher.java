package net.hypixel.nerdbot.marmalade.security;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Authenticated encryption for small PII fields (AES-256-GCM) plus a keyed HMAC for
 * building deterministic lookup hashes of the same values. Ciphertext format is
 * base64(nonce || ciphertext+tag) with a random 12-byte nonce per encryption, so
 * encrypting the same value twice never produces the same output. The HMAC key is
 * derived from the AES key rather than reused directly, keeping the two uses
 * cryptographically separate.
 */
public final class AesGcmCipher {

    private static final int KEY_LENGTH_BYTES = 32;
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] HMAC_SUBKEY_LABEL = "hmac-subkey".getBytes(StandardCharsets.UTF_8);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey aesKey;
    private final SecretKey hmacKey;

    public AesGcmCipher(byte[] key) {
        if (key == null || key.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("Key must be exactly " + KEY_LENGTH_BYTES + " bytes");
        }
        this.aesKey = new SecretKeySpec(key, "AES");
        this.hmacKey = deriveHmacKey(key);
    }

    public static AesGcmCipher fromBase64Key(String base64Key) {
        return new AesGcmCipher(Base64.getDecoder().decode(base64Key));
    }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(ciphertext, 0, combined, nonce.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            // AES-GCM with a valid key never fails to encrypt; treat as unrecoverable
            throw new IllegalStateException("AES-GCM encryption failed", e);
        }
    }

    public String decrypt(String encoded) throws CipherException {
        byte[] combined;
        try {
            combined = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new CipherException("Ciphertext is not valid base64", e);
        }

        if (combined.length <= NONCE_LENGTH_BYTES) {
            throw new CipherException("Ciphertext is too short to contain a nonce", null);
        }

        try {
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, combined, 0, NONCE_LENGTH_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
            byte[] plaintext = cipher.doFinal(combined, NONCE_LENGTH_BYTES, combined.length - NONCE_LENGTH_BYTES);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new CipherException("Decryption failed: tampered ciphertext or wrong key", e);
        }
    }

    public String hmac(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            return Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    private static SecretKey deriveHmacKey(byte[] key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return new SecretKeySpec(mac.doFinal(HMAC_SUBKEY_LABEL), HMAC_ALGORITHM);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC subkey derivation failed", e);
        }
    }
}
