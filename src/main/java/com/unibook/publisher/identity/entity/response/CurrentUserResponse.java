package com.unibook.publisher.identity.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.entity.User;

import java.util.UUID;

public record CurrentUserResponse(
        @JsonProperty("user_id")
        UUID userId,

        String email,

        @JsonProperty("display_name")
        String displayName,

        UserRole role,

        String bio,

        @JsonProperty("avatar_url")
        String avatarUrl,

        @JsonProperty("preferred_locale")
        String preferredLocale
) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getProfile().getDisplayName(),
                user.getRole(),
                user.getProfile().getBio(),
                user.getProfile().getAvatarUrl(),
                user.getProfile().getPreferredLocale()
        );
    }
}
