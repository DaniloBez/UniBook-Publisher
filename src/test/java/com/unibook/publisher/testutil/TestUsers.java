package com.unibook.publisher.testutil;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.security.AuthenticatedUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

public final class TestUsers {
    private TestUsers() {
    }

    public static AuthenticatedUser asUser(UUID id) {
        return asUser(id, UserRole.AUTHOR);
    }

    public static AuthenticatedUser asUser(UUID id, UserRole role) {
        return new AuthenticatedUser(id, id + "@test.local", "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
