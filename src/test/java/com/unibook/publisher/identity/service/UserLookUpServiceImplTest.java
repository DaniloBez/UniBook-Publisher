package com.unibook.publisher.identity.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.api.UserSummary;
import com.unibook.publisher.identity.entity.User;
import com.unibook.publisher.identity.entity.UserProfile;
import com.unibook.publisher.identity.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserLookUpServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupServiceImpl userLookupService;

    private User buildUser(UUID id, String email, String displayName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setHashedPassword("Hashed Password");
        user.setRole(UserRole.AUTHOR);

        UserProfile profile = new UserProfile();
        profile.setDisplayName(displayName);
        profile.setBio("Bio text");
        profile.setAvatarUrl("https://avatar.url/default.png");
        profile.setPreferredLocale("uk-UA");

        user.setProfile(profile);
        return user;
    }

    @Test
    @DisplayName("Повертає Optional.empty(), якщо id == null")
    void findById_NullId_ReturnsEmptyOptional() {
        Optional<UserSummary> result = userLookupService.findById(null);

        assertThat(result).isEmpty();
        verify(userRepository, never()).findByIdWithProfile(any());
    }

    @Test
    @DisplayName("Повертає UserSummary, якщо користувача знайдено")
    void findById_Success() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "test@unibook.com", "Test");

        when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));

        Optional<UserSummary> result = userLookupService.findById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(userId);
        assertThat(result.get().displayName()).isEqualTo("Test");
        assertThat(result.get().email()).isEqualTo("test@unibook.com");

        verify(userRepository, times(1)).findByIdWithProfile(userId);
    }

    @Test
    @DisplayName("Повертає Collections.emptyMap, якщо список ids null або порожній")
    void findAllByIds_NullOrEmptyIds_ReturnsEmptyMap() {
        Map<UUID, UserSummary> resultForNull = userLookupService.findAllByIds(null);
        Map<UUID, UserSummary> resultForEmpty = userLookupService.findAllByIds(Collections.emptyList());

        assertThat(resultForNull).isEmpty();
        assertThat(resultForEmpty).isEmpty();
        verify(userRepository, never()).findAllByIdWithProfile(any());
    }

    @Test
    @DisplayName("Повертає Map<UUID, UserSummary> для знайдених користувачів")
    void findAllByIds_Success() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        User user1 = buildUser(id1, "author@unibook.com", "Author One");
        User user2 = buildUser(id2, "editor@unibook.com", "Editor Two");

        Set<UUID> ids = Set.of(id1, id2);

        when(userRepository.findAllByIdWithProfile(ids)).thenReturn(List.of(user1, user2));

        Map<UUID, UserSummary> result = userLookupService.findAllByIds(ids);

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys(id1, id2);
        assertThat(result.get(id1).displayName()).isEqualTo("Author One");
        assertThat(result.get(id2).displayName()).isEqualTo("Editor Two");

        verify(userRepository, times(1)).findAllByIdWithProfile(ids);
    }
}
