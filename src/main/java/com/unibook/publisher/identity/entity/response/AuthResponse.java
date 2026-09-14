package com.unibook.publisher.identity.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.common.enums.UserRole;

import java.util.UUID;

public record AuthResponse(
        @JsonProperty("user_id")
        UUID userId,

        UserRole role,

        String token
) {
}
