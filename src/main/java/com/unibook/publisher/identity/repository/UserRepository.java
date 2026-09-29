package com.unibook.publisher.identity.repository;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.id = :id")
    Optional<User> findByIdWithProfile(@Param("id") UUID id);

    Optional<User> getByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile")
    List<User> findAllWithProfile();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.role = :role")
    List<User> findByRoleWithProfile(@Param("role") UserRole role);
}
