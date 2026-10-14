package com.unibook.publisher.identity.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to refresh an access token using a valid refresh token")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token issued at authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", maxLength = 2048)
        @NotBlank
        @Size(max = 2048)
        String refreshToken
) {}
