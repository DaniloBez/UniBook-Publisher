package com.unibook.publisher.identity.entity;

import java.util.UUID;

public record UserProfile(
        UUID userId,
        String displayName,
        String bio,
        String avatarUrl,
        String preferredLocale
) {
}
