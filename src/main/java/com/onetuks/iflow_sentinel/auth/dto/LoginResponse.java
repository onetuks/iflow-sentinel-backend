package com.onetuks.iflow_sentinel.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, String username, String role) {
}
