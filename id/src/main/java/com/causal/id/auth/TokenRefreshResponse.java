package com.causal.id.auth;

public record TokenRefreshResponse(String accessToken, String refreshToken) {
}
