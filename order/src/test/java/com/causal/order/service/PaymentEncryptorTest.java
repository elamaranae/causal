package com.causal.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PaymentEncryptorTest {

    private PaymentEncryptor encryptor;

    @BeforeEach
    void setUp() {
        // Generate a valid 32-byte AES-256 key
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) key[i] = (byte) i;
        String base64Key = Base64.getEncoder().encodeToString(key);
        encryptor = new PaymentEncryptor(base64Key);
    }

    @Test
    void encryptDecrypt_roundTrip() {
        String plaintext = "{\"type\":\"visa\",\"cardNumber\":\"4111111111111111\"}";
        String encrypted = encryptor.encrypt(plaintext);
        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_producesUniqueIV() {
        String plaintext = "same input";
        String encrypted1 = encryptor.encrypt(plaintext);
        String encrypted2 = encryptor.encrypt(plaintext);
        assertNotEquals(encrypted1, encrypted2, "Two encryptions of the same input should differ due to random IV");
        // Both should still decrypt to the same value
        assertEquals(plaintext, encryptor.decrypt(encrypted1));
        assertEquals(plaintext, encryptor.decrypt(encrypted2));
    }

    @Test
    void decrypt_tamperedCiphertext_throws() {
        String encrypted = encryptor.encrypt("sensitive data");
        byte[] combined = Base64.getDecoder().decode(encrypted);
        // Tamper with the ciphertext (after the 12-byte IV)
        combined[combined.length - 1] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(combined);
        assertThrows(RuntimeException.class, () -> encryptor.decrypt(tampered));
    }

    @Test
    void decrypt_wrongKey_throws() {
        String encrypted = encryptor.encrypt("secret");
        // Create a different encryptor with a different key
        byte[] otherKey = new byte[32];
        for (int i = 0; i < 32; i++) otherKey[i] = (byte) (i + 1);
        PaymentEncryptor otherEncryptor = new PaymentEncryptor(Base64.getEncoder().encodeToString(otherKey));
        assertThrows(RuntimeException.class, () -> otherEncryptor.decrypt(encrypted));
    }
}
