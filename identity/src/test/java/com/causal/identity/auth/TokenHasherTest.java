package com.causal.identity.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenHasherTest {

    @Test
    void hash_deterministicForSameInput() {
        String hash1 = TokenHasher.hash("test-token");
        String hash2 = TokenHasher.hash("test-token");
        assertEquals(hash1, hash2);
    }

    @Test
    void hash_differentInputsProduceDifferentHashes() {
        String hash1 = TokenHasher.hash("token-a");
        String hash2 = TokenHasher.hash("token-b");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void hash_producesValidHexString() {
        String hash = TokenHasher.hash("any-token");
        // SHA-256 produces 64 hex chars
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    void hash_emptyString_doesNotThrow() {
        assertDoesNotThrow(() -> TokenHasher.hash(""));
        assertNotNull(TokenHasher.hash(""));
    }
}
