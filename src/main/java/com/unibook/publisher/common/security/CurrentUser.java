package com.unibook.publisher.common.security;

import com.unibook.publisher.common.enums.UserRole;

import java.util.UUID;

public record CurrentUser(UUID id, UserRole role) {
}
