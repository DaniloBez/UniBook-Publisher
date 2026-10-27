package com.unibook.publisher.identity.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.common.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuthResponse(
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("user_id")
        UUID userId,

        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UserRole role,

        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("access_token")
        String accessToken,

        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("expires_in")
        Long expiresIn
) {}
