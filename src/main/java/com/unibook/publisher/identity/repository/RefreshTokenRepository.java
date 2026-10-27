package com.unibook.publisher.identity.repository;

import com.unibook.publisher.identity.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")
    void revokeAllByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.userId = :userId")
    void deleteAllByUserId(UUID userId);

    Optional<RefreshToken> findByJti(String jti);
}
