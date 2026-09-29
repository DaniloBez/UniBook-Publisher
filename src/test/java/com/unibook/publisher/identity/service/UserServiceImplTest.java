package com.unibook.publisher.identity.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.conflict.EmailAlreadyExistsException;
import com.unibook.publisher.common.exception.notfound.UserNotFoundException;
import com.unibook.publisher.common.exception.security.InvalidCredentialsException;
import com.unibook.publisher.identity.entity.User;
import com.unibook.publisher.identity.entity.UserProfile;
import com.unibook.publisher.identity.entity.request.LoginRequest;
import com.unibook.publisher.identity.entity.request.RegisterRequest;
import com.unibook.publisher.identity.entity.request.StaffRequest;
import com.unibook.publisher.identity.entity.request.UserProfileUpdateRequest;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.identity.entity.response.UserResponse;
import com.unibook.publisher.identity.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserServiceImpl userService;

    private User buildUser(UUID id, String email, String rawPassword, UserRole role, String displayName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setHashedPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        UserProfile profile = new UserProfile();
        profile.setId(UUID.randomUUID());
        profile.setDisplayName(displayName != null ? displayName : "Default Name");
        profile.setBio("Bio text");
        profile.setAvatarUrl("https://avatar.url/default.png");
        profile.setPreferredLocale("uk-UA");

        user.setProfile(profile);
        return user;
    }

    @Nested
    @DisplayName("Реєстрація користувача")
    class RegisterTests {

        @Test
        @DisplayName("Успішна реєстрація нового автора")
        void register_Success() {
            RegisterRequest request = new RegisterRequest(
                    "author@gmail.com",
                    "password123",
                    "Taras Shevchenko",
                    "Bio text",
                    "https://avatar.url/1.png",
                    "uk-UA"
            );
            UUID generatedId = UUID.randomUUID();
            User savedUser = buildUser(generatedId, request.email(), request.password(), UserRole.AUTHOR, request.displayName());

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            AuthResponse response = userService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(generatedId);
            assertThat(response.role()).isEqualTo(UserRole.AUTHOR);
            assertThat(response.token()).contains(generatedId.toString());

            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Помилка, коли пошта вже зайнята")
        void register_EmailAlreadyExists_ThrowsException() {
            RegisterRequest request = new RegisterRequest(
                    "author@gmail.com", "password", "Name", null, null, "uk-UA"
            );
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("вже існує");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Автентифікація")
    class LoginTests {

        @Test
        @DisplayName("Успішний вхід з правильними кредами")
        void login_Success() {
            String rawPassword = "password123";
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId, "author@gmail.com", rawPassword, UserRole.AUTHOR, "Artur");

            LoginRequest request = new LoginRequest("author@gmail.com", rawPassword);
            when(userRepository.getByEmail(request.email())).thenReturn(Optional.of(user));

            AuthResponse response = userService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.role()).isEqualTo(UserRole.AUTHOR);
        }

        @Test
        @DisplayName("Помилка, якщо користувача з такою поштою не існує")
        void login_UserNotFound_ThrowsException() {
            LoginRequest request = new LoginRequest("unknown@gmail.com", "pwd");
            when(userRepository.getByEmail(request.email())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Неправильна пошта або пароль");
        }

        @Test
        @DisplayName("Помилка, якщо пароль не збігається")
        void login_WrongPassword_ThrowsException() {
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId, "author@gmail.com", "correctPassword", UserRole.AUTHOR, "Artur");

            LoginRequest request = new LoginRequest("author@gmail.com", "wrongPassword");
            when(userRepository.getByEmail(request.email())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Неправильна пошта або пароль");
        }
    }

    @Nested
    @DisplayName("Отримання та оновлення профілю")
    class ProfileTests {

        @Test
        @DisplayName("Успішне отримання профілю користувача")
        void getUserProfile_Success() {
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId, "user@gmail.com", "pass", UserRole.AUTHOR, "User Display");

            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserProfile(userId);

            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.displayName()).isEqualTo("User Display");
            assertThat(response.role()).isEqualTo(UserRole.AUTHOR);
            assertThat(response.bio()).isEqualTo("Bio text");
        }

        @Test
        @DisplayName("Помилка отримання профілю, якщо користувача не знайдено")
        void getUserProfile_UserNotFound_ThrowsException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Успішне оновлення даних профілю")
        void updateUserProfile_Success() {
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId, "user@gmail.com", "pass", UserRole.AUTHOR, "Old Name");
            UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest("New Name", "New Bio", "new.png", "en-US");

            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userService.updateUserProfile(userId, updateRequest);

            assertThat(response.displayName()).isEqualTo("New Name");
            assertThat(response.bio()).isEqualTo("New Bio");
            assertThat(response.avatarUrl()).isEqualTo("new.png");
            assertThat(response.preferredLocale()).isEqualTo("en-US");

            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Помилка оновлення профілю, якщо користувача не знайдено")
        void updateUserProfile_UserNotFound_ThrowsException() {
            UUID userId = UUID.randomUUID();
            UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest("New Name", null, null, "en-US");

            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserProfile(userId, updateRequest))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Створення, фільтрація та видалення користувачів")
    class StaffFilterAndDeleteTests {

        @Test
        @DisplayName("Успішне створення співробітника адміністратором")
        void createStaff_Success() {
            StaffRequest request = new StaffRequest(
                    "editor@unibook.com", "securePass", UserRole.EDITOR, "Editor John", "Head of Editing", null, "uk-UA"
            );
            UUID generatedId = UUID.randomUUID();
            User savedUser = buildUser(generatedId, request.email(), request.password(), UserRole.EDITOR, request.displayName());

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserResponse response = userService.createStaff(request);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(generatedId);
            assertThat(response.role()).isEqualTo(UserRole.EDITOR);
            assertThat(response.displayName()).isEqualTo("Editor John");

            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Помилка створення співробітника, якщо пошта вже зайнята")
        void createStaff_EmailAlreadyExists_ThrowsException() {
            StaffRequest request = new StaffRequest(
                    "editor@unibook.com", "securePass", UserRole.EDITOR, "Editor John", "Head of Editing", null, "uk-UA"
            );

            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            assertThatThrownBy(() -> userService.createStaff(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("вже існує");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Отримання користувачів за заданою роллю з підвантаженням профілю")
        void getUsers_FilteredByRole() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id, "editor@unibook.com", "pass", UserRole.EDITOR, "Editor John");

            when(userRepository.findByRoleWithProfile(UserRole.EDITOR)).thenReturn(List.of(user));

            List<UserResponse> result = userService.getUsers(UserRole.EDITOR);

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.getFirst().role()).isEqualTo(UserRole.EDITOR);
            assertThat(result.getFirst().displayName()).isEqualTo("Editor John");

            verify(userRepository, never()).findAllWithProfile();
        }

        @Test
        @DisplayName("Повертає порожній список, якщо за роллю не знайдено жодного користувача")
        void getUsers_EmptyResult_ReturnsEmptyList() {
            when(userRepository.findByRoleWithProfile(UserRole.ADMIN)).thenReturn(List.of());

            List<UserResponse> result = userService.getUsers(UserRole.ADMIN);

            assertThat(result).isNotNull();
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Отримання всіх користувачів з профілями, якщо роль не передано")
        void getUsers_AllUsers_WhenRoleIsNull() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id, "user@gmail.com", "pass", UserRole.AUTHOR, "Author One");

            when(userRepository.findAllWithProfile()).thenReturn(List.of(user));

            List<UserResponse> result = userService.getUsers(null);

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.getFirst().displayName()).isEqualTo("Author One");

            verify(userRepository, times(1)).findAllWithProfile();
        }

        @Test
        @DisplayName("Успішне видалення користувача за ID")
        void deleteUser_Success() {
            UUID userId = UUID.randomUUID();
            when(userRepository.existsById(userId)).thenReturn(true);

            userService.deleteUser(userId);

            verify(userRepository, times(1)).deleteById(userId);
        }

        @Test
        @DisplayName("Помилка видалення, якщо користувача з таким ID не існує")
        void deleteUser_NotFound_ThrowsException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(userId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).deleteById(any());
        }
    }
}
