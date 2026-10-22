package com.unibook.publisher.identity.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.entity.User;

import java.util.UUID;

public record UserSummary(
        UUID id,

        @JsonProperty("display_name")
        String displayName,

        String email,

        UserRole role,

        @JsonProperty("avatar_url")
        String avatarUrl
) {
    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getProfile().getDisplayName(),
                user.getEmail(),
                user.getRole(),
                user.getProfile().getAvatarUrl()
        );
    }
}
