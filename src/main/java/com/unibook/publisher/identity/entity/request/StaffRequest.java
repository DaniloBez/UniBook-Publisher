package com.unibook.publisher.identity.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.common.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StaffRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        String password,

        @NotNull
        UserRole role,

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
