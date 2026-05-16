package com.causal.identity.auth;

public record TokenRefreshResponse(String accessToken, String refreshToken) {
}
