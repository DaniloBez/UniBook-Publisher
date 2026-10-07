package com.unibook.publisher.identity.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User login credentials")
public record LoginRequest(
        @Schema(description = "User email address", example = "author@example.com", maxLength = 255)
        @Email
        @NotBlank
        @Size(max = 255)
        String email,

        @Schema(description = "User password", example = "StrongPass123", minLength = 8, maxLength = 72, format = "password")
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
