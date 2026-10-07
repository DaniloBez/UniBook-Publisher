package com.unibook.publisher.identity.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "Request to register a new author account")
public record RegisterRequest(
        @Schema(description = "Email address, used as the login", example = "author@example.com", maxLength = 255)
        @Email
        @NotBlank
        @Size(max = 255)
        String email,

        @Schema(description = "Account password", example = "StrongPass123", minLength = 8, maxLength = 72, format = "password")
        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Schema(description = "Name shown in the user profile", example = "Taras Shevchenko", minLength = 2, maxLength = 100)
        @NotBlank
        @Size(min = 2, max = 100)
        @JsonProperty("display_name")
        String displayName,

        @Schema(description = "Short biography", example = "Poet and prose writer", maxLength = 1000, nullable = true)
        @Size(max = 1000)
        String bio,

        @Schema(description = "URL of the avatar image", example = "https://example.com/avatars/taras.png", maxLength = 2048, nullable = true)
        @Size(max = 2048)
        @URL
        @JsonProperty("avatar_url")
        String avatarUrl,

        @Schema(description = "Preferred interface locale: 'uk' or 'uk-UA'", example = "uk-UA", maxLength = 5, nullable = true)
        @Size(max = 5)
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Формат: 'uk' або 'uk-UA'")
        @JsonProperty("preferred_locale")
        String preferredLocale
) {
}
