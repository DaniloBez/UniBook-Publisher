package com.unibook.publisher.identity.repository;

import com.unibook.publisher.identity.entity.UserProfile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserProfileRepository {
    private final Map<UUID, UserProfile> userProfiles = new ConcurrentHashMap<>();

    public Optional<UserProfile> get(UUID userId) {
        return Optional.ofNullable(userProfiles.get(userId));
    }

    public UserProfile save(UserProfile profile) {
        userProfiles.put(profile.userId(), profile);
        return profile;
    }

    public Optional<UserProfile> update(UUID userId, UserProfile updatedProfile) {
        if (!userProfiles.containsKey(userId))
            return Optional.empty();

        UserProfile toSave = new UserProfile(
                userId,
                updatedProfile.displayName(),
                updatedProfile.bio(),
                updatedProfile.avatarUrl(),
                updatedProfile.preferredLocale()
        );
        userProfiles.put(userId, toSave);
        return Optional.of(toSave);
    }

    public void delete(UUID userId) {
        userProfiles.remove(userId);
    }
}
