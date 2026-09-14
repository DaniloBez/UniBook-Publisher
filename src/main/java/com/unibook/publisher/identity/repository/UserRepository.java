package com.unibook.publisher.identity.repository;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.entity.User;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {
    private final Map<UUID, User> users = new ConcurrentHashMap<>();

    public Optional<User> get(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    public Optional<User> getByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.email().equalsIgnoreCase(email))
                .findFirst();
    }

    public boolean existsByEmail(String email) {
        return users.values().stream()
                .anyMatch(u -> u.email().equalsIgnoreCase(email));
    }

    public User save(User user) {
        UUID id = UUID.randomUUID();
        User newUser = new User(id, user.email(), user.hashedPassword(), user.role());
        users.put(id, newUser);
        return newUser;
    }

    public Optional<User> update(UUID id, User updatedUser) {
        if (!users.containsKey(id)) {
            return Optional.empty();
        }
        User toSave = new User(id, updatedUser.email(), updatedUser.hashedPassword(), updatedUser.role());
        users.put(id, toSave);
        return Optional.of(toSave);
    }

    public void delete(UUID id) {
        users.remove(id);
    }

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public List<User> findByRole(UserRole role) {
        return users.values().stream()
                .filter(u -> u.role() == role)
                .toList();
    }
}
