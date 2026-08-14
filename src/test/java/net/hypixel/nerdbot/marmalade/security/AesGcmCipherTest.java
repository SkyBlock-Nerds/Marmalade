package net.hypixel.nerdbot.marmalade.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmCipherTest {

    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes();

    @Test
    void roundTripsPlaintext() throws CipherException {
        AesGcmCipher cipher = new AesGcmCipher(KEY);
        String ciphertext = cipher.encrypt("user@example.com");
        assertThat(ciphertext).isNotEqualTo("user@example.com");
        assertThat(cipher.decrypt(ciphertext)).isEqualTo("user@example.com");
    }

    @Test
    void producesDifferentCiphertextEachTime() {
        AesGcmCipher cipher = new AesGcmCipher(KEY);
        assertThat(cipher.encrypt("same input")).isNotEqualTo(cipher.encrypt("same input"));
    }

    @Test
    void rejectsTamperedCiphertext() {
        AesGcmCipher cipher = new AesGcmCipher(KEY);
        byte[] raw = Base64.getDecoder().decode(cipher.encrypt("user@example.com"));
        raw[raw.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(raw);
        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(CipherException.class);
    }

    @Test
    void rejectsCiphertextFromDifferentKey() {
        String ciphertext = new AesGcmCipher(KEY).encrypt("user@example.com");
        AesGcmCipher other = new AesGcmCipher("fedcba9876543210fedcba9876543210".getBytes());
        assertThatThrownBy(() -> other.decrypt(ciphertext)).isInstanceOf(CipherException.class);
    }

    @Test
    void rejectsGarbageCiphertext() {
        AesGcmCipher cipher = new AesGcmCipher(KEY);
        assertThatThrownBy(() -> cipher.decrypt("not-base64!!!")).isInstanceOf(CipherException.class);
        assertThatThrownBy(() -> cipher.decrypt("c2hvcnQ=")).isInstanceOf(CipherException.class); // shorter than a nonce
    }

    @Test
    void rejectsWrongKeyLength() {
        assertThatThrownBy(() -> new AesGcmCipher("short".getBytes())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AesGcmCipher(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loadsKeyFromBase64() throws CipherException {
        String base64Key = Base64.getEncoder().encodeToString(KEY);
        AesGcmCipher cipher = AesGcmCipher.fromBase64Key(base64Key);
        assertThat(cipher.decrypt(cipher.encrypt("x"))).isEqualTo("x");
    }

    @Test
    void hmacIsDeterministicAndInputSensitive() {
        AesGcmCipher cipher = new AesGcmCipher(KEY);
        assertThat(cipher.hmac("user@example.com")).isEqualTo(cipher.hmac("user@example.com"));
        assertThat(cipher.hmac("user@example.com")).isNotEqualTo(cipher.hmac("other@example.com"));
    }

    @Test
    void hmacDiffersAcrossKeys() {
        AesGcmCipher a = new AesGcmCipher(KEY);
        AesGcmCipher b = new AesGcmCipher("fedcba9876543210fedcba9876543210".getBytes());
        assertThat(a.hmac("user@example.com")).isNotEqualTo(b.hmac("user@example.com"));
    }
}
