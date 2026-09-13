package com.unibook.publisher.identity.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.common.enums.UserRole;

public record UserResponse(
        @JsonProperty("display_name")
        String displayName,

        UserRole role,

        String bio,

        @JsonProperty("avatar_url")
        String avatarUrl,

        @JsonProperty("preferred_locale")
        String preferredLocale
) {
}
