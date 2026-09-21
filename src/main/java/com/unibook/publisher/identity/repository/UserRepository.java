package com.unibook.publisher.identity.repository;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> get(UUID id);

    Optional<User> getByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);

    List<User> findAll();

    List<User> findByRole(UserRole role);
}
