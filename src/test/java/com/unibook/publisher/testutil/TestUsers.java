package com.unibook.publisher.testutil;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.entity.User;

import java.util.UUID;

public final class TestUsers {
    private TestUsers() {
    }

    public static User asUser(UUID id) {
        return asUser(id, UserRole.AUTHOR);
    }

    public static User asUser(UUID id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test.local");
        user.setRole(role);
        return user;
    }
}
