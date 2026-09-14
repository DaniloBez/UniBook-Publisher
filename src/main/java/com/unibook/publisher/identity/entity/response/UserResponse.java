package com.unibook.publisher.identity.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.entity.User;
import com.unibook.publisher.identity.entity.UserProfile;

import java.util.UUID;

public record UserResponse(
        @JsonProperty("user_id")
        UUID userId,

        @JsonProperty("display_name")
        String displayName,

        UserRole role,

        String bio,

        @JsonProperty("avatar_url")
        String avatarUrl,

        @JsonProperty("preferred_locale")
        String preferredLocale
) {
        public static UserResponse from(User user, UserProfile profile) {
                return new UserResponse(
                        user.id(),
                        profile != null ? profile.displayName() : null,
                        user.role(),
                        profile != null ? profile.bio() : null,
                        profile != null ? profile.avatarUrl() : null,
                        profile != null ? profile.preferredLocale() : null
                );
        }
}
