package com.unibook.publisher.identity.entity.response;

public record AuthResult(
        AuthResponse response,
        String refreshToken
) {
}
