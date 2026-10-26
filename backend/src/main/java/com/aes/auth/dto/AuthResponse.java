package com.aes.auth.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        Long userId,
        String name,
        String email,
        String role
) {
    public AuthResponse(String token, Long userId, String name, String email, String role) {
        this(token, null, userId, name, email, role);
    }
}
