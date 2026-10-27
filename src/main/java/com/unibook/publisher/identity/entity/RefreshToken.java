package com.unibook.publisher.identity.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_jti", columnList = "jti"),
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Setter
    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private final Instant createdAt = Instant.now();

    public RefreshToken(String jti, UUID userId, Instant expiresAt) {
        this.jti = jti;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }
}