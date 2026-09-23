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
import com.unibook.publisher.identity.repository.UserProfileRepository;
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

    @Mock
    private UserProfileRepository userProfileRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserServiceImpl userService;

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
            User savedUser = new User(generatedId, request.email(), "encodedPassword", UserRole.AUTHOR);

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            AuthResponse response = userService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(generatedId);
            assertThat(response.role()).isEqualTo(UserRole.AUTHOR);
            assertThat(response.token()).contains(generatedId.toString());

            verify(userProfileRepository, times(1)).save(any(UserProfile.class));
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
            verify(userProfileRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Автентифікація")
    class LoginTests {

        @Test
        @DisplayName("Успішний вхід з правильними кредами")
        void login_Success() {
            String rawPassword = "password123";
            String hashedPassword = passwordEncoder.encode(rawPassword);
            UUID userId = UUID.randomUUID();
            User user = new User(userId, "author@gmail.com", hashedPassword, UserRole.AUTHOR);

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
            String hashedPassword = passwordEncoder.encode("correctPassword");
            User user = new User(UUID.randomUUID(), "author@gmail.com", hashedPassword, UserRole.AUTHOR);

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
            User user = new User(userId, "user@gmail.com", "hash", UserRole.AUTHOR);
            UserProfile profile = new UserProfile(userId, "User Display", "My bio", "avatar.png", "uk-UA");

            when(userRepository.get(userId)).thenReturn(Optional.of(user));
            when(userProfileRepository.get(userId)).thenReturn(Optional.of(profile));

            UserResponse response = userService.getUserProfile(userId);

            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.displayName()).isEqualTo("User Display");
            assertThat(response.role()).isEqualTo(UserRole.AUTHOR);
            assertThat(response.bio()).isEqualTo("My bio");
        }

        @Test
        @DisplayName("Помилка отримання профілю, якщо користувача не знайдено")
        void getUserProfile_UserNotFound_ThrowsException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.get(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Помилка отримання профілю, якщо профіль користувача відсутній")
        void getUserProfile_ProfileNotFound_ThrowsException() {
            UUID userId = UUID.randomUUID();
            User user = new User(userId, "user@gmail.com", "hash", UserRole.AUTHOR);

            when(userRepository.get(userId)).thenReturn(Optional.of(user));
            when(userProfileRepository.get(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Успішне оновлення даних профілю")
        void updateUserProfile_Success() {
            UUID userId = UUID.randomUUID();
            User user = new User(userId, "user@gmail.com", "hash", UserRole.AUTHOR);
            UserProfile updatedProfile = new UserProfile(userId, "New Name", "New Bio", "new.png", "en-US");
            UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest("New Name", "New Bio", "new.png", "en-US");

            when(userRepository.get(userId)).thenReturn(Optional.of(user));
            when(userProfileRepository.update(eq(userId), any(UserProfile.class))).thenReturn(Optional.of(updatedProfile));

            UserResponse response = userService.updateUserProfile(userId, updateRequest);

            assertThat(response.displayName()).isEqualTo("New Name");
            assertThat(response.preferredLocale()).isEqualTo("en-US");
        }

        @Test
        @DisplayName("Помилка оновлення профілю, якщо користувача не знайдено")
        void updateUserProfile_UserNotFound_ThrowsException() {
            UUID userId = UUID.randomUUID();
            UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest("New Name", null, null, "en-US");

            when(userRepository.get(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserProfile(userId, updateRequest))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userProfileRepository, never()).update(any(), any());
        }

        @Test
        @DisplayName("Помилка оновлення профілю, якщо запис профілю не знайдено для оновлення")
        void updateUserProfile_ProfileNotFound_ThrowsException() {
            UUID userId = UUID.randomUUID();
            User user = new User(userId, "user@gmail.com", "hash", UserRole.AUTHOR);
            UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest("New Name", null, null, "en-US");

            when(userRepository.get(userId)).thenReturn(Optional.of(user));
            when(userProfileRepository.update(eq(userId), any(UserProfile.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserProfile(userId, updateRequest))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Створення та фільтрація співробітників")
    class StaffAndFilterTests {

        @Test
        @DisplayName("Успішне створення співробітника адміністратором")
        void createStaff_Success() {
            StaffRequest request = new StaffRequest(
                    "editor@unibook.com", "securePass", UserRole.EDITOR, "Editor John", "Head of Editing", null, "uk-UA"
            );
            UUID generatedId = UUID.randomUUID();
            User savedUser = new User(generatedId, request.email(), "hash", UserRole.EDITOR);

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userService.createStaff(request);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(generatedId);
            assertThat(response.role()).isEqualTo(UserRole.EDITOR);
            verify(userProfileRepository, times(1)).save(any(UserProfile.class));
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
            verify(userProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("Отримання користувачів за заданою роллю")
        void getUsers_FilteredByRole() {
            UUID id = UUID.randomUUID();
            User user = new User(id, "editor@unibook.com", "hash", UserRole.EDITOR);
            UserProfile profile = new UserProfile(id, "Editor John", null, null, "uk-UA");

            when(userRepository.findByRole(UserRole.EDITOR)).thenReturn(List.of(user));
            when(userProfileRepository.get(id)).thenReturn(Optional.of(profile));

            List<UserResponse> result = userService.getUsers(UserRole.EDITOR);

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.getFirst().role()).isEqualTo(UserRole.EDITOR);
            assertThat(result.getFirst().displayName()).isEqualTo("Editor John");
            verify(userRepository, never()).findAll();
        }

        @Test
        @DisplayName("Повертає порожній список, якщо за роллю не знайдено жодного користувача")
        void getUsers_EmptyResult_ReturnsEmptyList() {
            when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of());

            List<UserResponse> result = userService.getUsers(UserRole.ADMIN);

            assertThat(result).isNotNull();
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Отримання всіх користувачів, якщо роль не передано")
        void getUsers_AllUsers_WhenRoleIsNull() {
            UUID id = UUID.randomUUID();
            User user = new User(id, "user@gmail.com", "hash", UserRole.AUTHOR);

            when(userRepository.findAll()).thenReturn(List.of(user));
            when(userProfileRepository.get(id)).thenReturn(Optional.empty());

            List<UserResponse> result = userService.getUsers(null);

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.getFirst().displayName()).isNull();
            verify(userRepository, times(1)).findAll();
        }
    }
}
