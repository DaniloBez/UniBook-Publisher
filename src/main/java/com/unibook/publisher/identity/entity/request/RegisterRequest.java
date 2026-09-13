package com.unibook.publisher.identity.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        String password,

        @NotBlank
        @JsonProperty("display_name")
        String displayName,

        String bio,

        @JsonProperty("avatar_url")
        String avatarUrl,

        @JsonProperty("preferredLocale")
        String preferredLocale
) {
}
