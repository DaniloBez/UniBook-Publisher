package com.unibook.publisher.identity.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.conflict.EmailAlreadyExistsException;
import com.unibook.publisher.common.exception.notfound.UserNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.security.InvalidCredentialsException;
import com.unibook.publisher.common.security.jwt.JwtService;
import com.unibook.publisher.identity.entity.RefreshToken;
import com.unibook.publisher.identity.entity.User;
import com.unibook.publisher.identity.entity.UserProfile;
import com.unibook.publisher.identity.entity.request.*;
import com.unibook.publisher.identity.entity.response.AuthResult;
import com.unibook.publisher.identity.entity.response.CurrentUserResponse;
import com.unibook.publisher.identity.entity.response.UserResponse;
import com.unibook.publisher.identity.repository.RefreshTokenRepository;
import com.unibook.publisher.identity.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.unibook.publisher.testutil.TestUsers.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

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

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(request.email())
                    .password(request.password())
                    .roles("AUTHOR")
                    .build();
            when(userDetailsService.loadUserByUsername(request.email())).thenReturn(userDetails);
            when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(UserDetails.class), anyString())).thenReturn("refresh-token");
            when(jwtService.getAccessExpirationInSeconds()).thenReturn(1800L);
            when(jwtService.getRefreshExpirationInSeconds()).thenReturn(604800L);

            AuthResult result = userService.register(request);

            assertThat(result).isNotNull();
            assertThat(result.response().userId()).isEqualTo(generatedId);
            assertThat(result.response().role()).isEqualTo(UserRole.AUTHOR);
            assertThat(result.response().accessToken()).isNotNull();
            assertThat(result.response().expiresIn()).isNotNull();
            assertThat(result.refreshToken()).isEqualTo("refresh-token");

            verify(userRepository, times(1)).save(any(User.class));
            verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
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
            verify(refreshTokenRepository, never()).save(any());
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

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(request.email())
                    .password(request.password())
                    .roles("AUTHOR")
                    .build();
            when(userDetailsService.loadUserByUsername(request.email())).thenReturn(userDetails);
            when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(UserDetails.class), anyString())).thenReturn("refresh-token");
            when(jwtService.getAccessExpirationInSeconds()).thenReturn(1800L);
            when(jwtService.getRefreshExpirationInSeconds()).thenReturn(604800L);

            AuthResult result = userService.login(request);

            assertThat(result).isNotNull();
            assertThat(result.response().userId()).isEqualTo(userId);
            assertThat(result.response().role()).isEqualTo(UserRole.AUTHOR);
            assertThat(result.response().accessToken()).isNotNull();
            assertThat(result.response().expiresIn()).isNotNull();
            assertThat(result.refreshToken()).isEqualTo("refresh-token");

            verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
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
    @DisplayName("Оновлення токенів та Stateful перевірка")
    class RefreshTokenTests {

        @Test
        @DisplayName("Успішна ротація токенів")
        void refreshToken_Success() {
            UUID userId = UUID.randomUUID();
            String refreshToken = "valid-refresh-token";
            String jti = "valid-jti";
            String email = "author@gmail.com";
            User user = buildUser(userId, email, "password123", UserRole.AUTHOR, "Artur");

            RefreshToken tokenEntity = new RefreshToken(jti, userId, Instant.now().plusSeconds(3600));

            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtService.extractJti(refreshToken)).thenReturn(jti);
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(tokenEntity));
            when(jwtService.extractUsername(refreshToken)).thenReturn(email);
            when(userRepository.getByEmail(email)).thenReturn(Optional.of(user));

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("password123")
                    .roles("AUTHOR")
                    .build();

            when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
            when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken(any(UserDetails.class), anyString())).thenReturn("new-refresh-token");
            when(jwtService.getAccessExpirationInSeconds()).thenReturn(1800L);
            when(jwtService.getRefreshExpirationInSeconds()).thenReturn(604800L);

            AuthResult result = userService.refreshToken(refreshToken);

            assertThat(result).isNotNull();
            assertThat(result.response().userId()).isEqualTo(userId);
            assertThat(result.response().role()).isEqualTo(UserRole.AUTHOR);
            assertThat(result.response().accessToken()).isEqualTo("new-access-token");
            assertThat(result.response().expiresIn()).isEqualTo(1800L);
            assertThat(result.refreshToken()).isEqualTo("new-refresh-token");

            assertThat(tokenEntity.isRevoked()).isTrue();
            verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Виявлення повторного використання (Reuse Detection): відкликаються всі сесії при спробі використати анульований токен")
        void refreshToken_AlreadyRevoked_RevokesAllUserTokensAndThrowsException() {
            UUID userId = UUID.randomUUID();
            String refreshToken = "revoked-refresh-token";
            String jti = "revoked-jti";

            RefreshToken tokenEntity = new RefreshToken(jti, userId, Instant.now().plusSeconds(3600));
            tokenEntity.setRevoked(true);

            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtService.extractJti(refreshToken)).thenReturn(jti);
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(tokenEntity));

            assertThatThrownBy(() -> userService.refreshToken(refreshToken))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(refreshTokenRepository, times(1)).revokeAllByUserId(userId);
            verify(userRepository, never()).getByEmail(any());
        }

        @Test
        @DisplayName("Помилка, якщо токен протерміновано в БД")
        void refreshToken_ExpiredToken_MarksRevokedAndThrowsException() {
            UUID userId = UUID.randomUUID();
            String refreshToken = "expired-refresh-token";
            String jti = "expired-jti";

            RefreshToken tokenEntity = new RefreshToken(jti, userId, Instant.now().minusSeconds(10));

            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtService.extractJti(refreshToken)).thenReturn(jti);
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(tokenEntity));

            assertThatThrownBy(() -> userService.refreshToken(refreshToken))
                    .isInstanceOf(InvalidCredentialsException.class);

            assertThat(tokenEntity.isRevoked()).isTrue();
            verify(userRepository, never()).getByEmail(any());
        }

        @Test
        @DisplayName("Помилка, якщо refresh token недійсний за структурою JWT")
        void refreshToken_InvalidJwtStructure_ThrowsException() {
            String refreshToken = "invalid-refresh-token";

            when(jwtService.isRefreshToken(refreshToken)).thenReturn(false);

            assertThatThrownBy(() -> userService.refreshToken(refreshToken))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(refreshTokenRepository, never()).findByJti(any());
            verify(userRepository, never()).getByEmail(any());
        }
    }

    @Nested
    @DisplayName("Вихід із системи (Logout)")
    class LogoutTests {

        @Test
        @DisplayName("Успішний logout: токен позначається як відкликаний")
        void logout_Success() {
            UUID userId = UUID.randomUUID();
            String refreshToken = "valid-refresh-token";
            String jti = "valid-jti";

            RefreshToken tokenEntity = new RefreshToken(jti, userId, Instant.now().plusSeconds(3600));

            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtService.extractJti(refreshToken)).thenReturn(jti);
            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(tokenEntity));

            userService.logout(refreshToken);

            assertThat(tokenEntity.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("Logout з порожнім токеном не робить дій в БД")
        void logout_NullOrBlankToken_DoesNothing() {
            userService.logout(null);
            userService.logout("  ");

            verifyNoInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Logout з недійсним токеном обробляється мовчки без винятку")
        void logout_InvalidToken_HandlesGracefully() {
            String refreshToken = "malformed-token";
            when(jwtService.isRefreshToken(refreshToken)).thenThrow(new RuntimeException("Parsing failed"));

            userService.logout(refreshToken);

            verify(refreshTokenRepository, never()).findByJti(any());
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
        @DisplayName("Успішне отримання поточного профілю користувача")
        void getCurrentUser_Success() {
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId, "user@gmail.com", "pass", UserRole.AUTHOR, "User Display");

            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));

            CurrentUserResponse response = userService.getCurrentUser(userId);

            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.email()).isEqualTo("user@gmail.com");
            assertThat(response.displayName()).isEqualTo("User Display");
            assertThat(response.role()).isEqualTo(UserRole.AUTHOR);
            assertThat(response.bio()).isEqualTo("Bio text");
        }

        @Test
        @DisplayName("Помилка отримання поточного профілю користувача, якщо користувача не знайдено")
        void getCurrentUser_UserNotFound_ThrowsException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getCurrentUser(userId))
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

            UserResponse response = userService.updateUserProfile(userId, asUser(userId), updateRequest);

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

            assertThatThrownBy(() -> userService.updateUserProfile(userId, asUser(userId), updateRequest))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Помилка оновлення профілю, якщо користувач не має права редагувати профіль")
        void updateUserProfile_ForbittenAction_ThrowsException() {
            UUID profileUserId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            UserProfileUpdateRequest updateRequest = new UserProfileUpdateRequest("New Name", "New Bio", "new.png", "en-US");

            assertThatThrownBy(() -> userService.updateUserProfile(profileUserId, asUser(userId), updateRequest))
                    .isInstanceOf(ForbiddenActionException.class);

            verify(userRepository, never()).findByIdWithProfile(any());
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

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().role()).isEqualTo(UserRole.EDITOR);
            assertThat(result.getFirst().displayName()).isEqualTo("Editor John");

            verify(userRepository, never()).findAllWithProfile();
        }

        @Test
        @DisplayName("Повертає порожній список, якщо за роллю не знайдено жодного користувача")
        void getUsers_EmptyResult_ReturnsEmptyList() {
            when(userRepository.findByRoleWithProfile(UserRole.ADMIN)).thenReturn(List.of());

            List<UserResponse> result = userService.getUsers(UserRole.ADMIN);

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Отримання всіх користувачів з профілями, якщо роль не передано")
        void getUsers_AllUsers_WhenRoleIsNull() {
            UUID id = UUID.randomUUID();
            User user = buildUser(id, "user@gmail.com", "pass", UserRole.AUTHOR, "Author One");

            when(userRepository.findAllWithProfile()).thenReturn(List.of(user));

            List<UserResponse> result = userService.getUsers(null);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().displayName()).isEqualTo("Author One");

            verify(userRepository, times(1)).findAllWithProfile();
        }

        @Test
        @DisplayName("Успішне видалення користувача за ID (також видаляються всі його токени)")
        void deleteUser_Success() {
            UUID userId = UUID.randomUUID();
            when(userRepository.existsById(userId)).thenReturn(true);

            userService.deleteUser(userId);

            verify(refreshTokenRepository, times(1)).deleteAllByUserId(userId);
            verify(userRepository, times(1)).deleteById(userId);
        }

        @Test
        @DisplayName("Помилка видалення, якщо користувача з таким ID не існує")
        void deleteUser_NotFound_ThrowsException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(userId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(refreshTokenRepository, never()).deleteAllByUserId(any());
            verify(userRepository, never()).deleteById(any());
        }
    }
}