package com.unibook.publisher.identity.entity;

import com.unibook.publisher.common.enums.UserRole;

import java.util.UUID;

public record User(
        UUID id,
        String email,
        String hashedPassword,
        UserRole role
) {
}
