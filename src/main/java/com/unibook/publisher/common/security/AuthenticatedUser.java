package com.unibook.publisher.common.security;

import com.unibook.publisher.common.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String password,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public UserRole getUserRole() {
        if(authorities == null || authorities.isEmpty()) {
            return null;
        }
        String authority = authorities.iterator().next().getAuthority();
        String role = authority.startsWith("ROLE_") ? authority.substring("ROLE_".length()) : authority;
        return UserRole.valueOf(role);
    }
}
