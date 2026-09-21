package com.unibook.publisher.identity.repository;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.entity.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<UUID, User> users = new ConcurrentHashMap<>();

    @Override
    public Optional<User> get(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> getByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.email().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.values().stream()
                .anyMatch(u -> u.email().equalsIgnoreCase(email));
    }

    @Override
    public User save(User user) {
        UUID id = UUID.randomUUID();
        User newUser = new User(id, user.email(), user.hashedPassword(), user.role());
        users.put(id, newUser);
        return newUser;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public List<User> findByRole(UserRole role) {
        return users.values().stream()
                .filter(u -> u.role() == role)
                .toList();
    }
}
