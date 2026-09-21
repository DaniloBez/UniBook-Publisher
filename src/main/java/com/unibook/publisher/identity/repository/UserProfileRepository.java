package com.unibook.publisher.identity.repository;

import com.unibook.publisher.identity.entity.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {

    Optional<UserProfile> get(UUID userId);

    UserProfile save(UserProfile profile);

    Optional<UserProfile> update(UUID userId, UserProfile updatedProfile);
}
