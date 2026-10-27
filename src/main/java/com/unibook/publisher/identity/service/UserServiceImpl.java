package com.unibook.publisher.identity.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.conflict.EmailAlreadyExistsException;
import com.unibook.publisher.common.exception.notfound.UserNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.security.InvalidCredentialsException;
import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.common.security.jwt.JwtService;
import com.unibook.publisher.identity.entity.RefreshToken;
import com.unibook.publisher.identity.entity.User;
import com.unibook.publisher.identity.entity.UserProfile;
import com.unibook.publisher.identity.entity.request.*;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.identity.entity.response.AuthResult;
import com.unibook.publisher.identity.entity.response.CurrentUserResponse;
import com.unibook.publisher.identity.entity.response.UserResponse;
import com.unibook.publisher.identity.repository.RefreshTokenRepository;
import com.unibook.publisher.identity.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserServiceImpl(
            UserRepository userRepository,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email()))
            throw new EmailAlreadyExistsException(request.email());

        User user = new User();
        user.setEmail(request.email());
        user.setHashedPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.AUTHOR);

        UserProfile profile = new UserProfile();
        profile.setDisplayName(request.displayName());
        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setPreferredLocale(request.preferredLocale());

        user.setProfile(profile);

        User savedUser = userRepository.save(user);

        return toAuthResult(savedUser);
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = userRepository.getByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getHashedPassword()))
            throw new InvalidCredentialsException();

        return toAuthResult(user);
    }

    @Override
    @Transactional
    public AuthResult refreshToken(String refreshToken) {
        if (refreshToken == null || !jwtService.isRefreshToken(refreshToken))
            throw new InvalidCredentialsException();

        String jti;
        try {
            jti = jwtService.extractJti(refreshToken);
        } catch (Exception e) {
            throw new InvalidCredentialsException();
        }

        RefreshToken token = refreshTokenRepository.findByJti(jti)
                .orElseThrow(InvalidCredentialsException::new);

        if (token.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(token.getUserId());
            throw new InvalidCredentialsException();
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            token.setRevoked(true);
            throw new InvalidCredentialsException();
        }

        token.setRevoked(true);

        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.getByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        return toAuthResult(user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;

        try {
            if (jwtService.isRefreshToken(refreshToken)) {
                String jti = jwtService.extractJti(refreshToken);
                refreshTokenRepository.findByJti(jti).ifPresent(token -> token.setRevoked(true));
            }
        } catch (Exception _) { }
    }

    @Override
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserResponse.from(user);
    }

    @Override
    public CurrentUserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return CurrentUserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(UUID profileUserId, AuthenticatedUser currentUser, UserProfileUpdateRequest request) {
        if (!currentUser.id().equals(profileUserId))
            throw new ForbiddenActionException("Користувач має право редагувати тільки свій профіль");

        User user = userRepository.findByIdWithProfile(profileUserId)
                .orElseThrow(() -> new UserNotFoundException(profileUserId));

        UserProfile profile = user.getProfile();

        profile.setDisplayName(request.displayName());
        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setPreferredLocale(request.preferredLocale());

        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createStaff(StaffRequest request) {
        if (userRepository.existsByEmail(request.email()))
            throw new EmailAlreadyExistsException(request.email());

        User user = new User();
        user.setEmail(request.email());
        user.setHashedPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        UserProfile profile = new UserProfile();
        profile.setDisplayName(request.displayName());
        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setPreferredLocale(request.preferredLocale());

        user.setProfile(profile);

        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CHIEF_EDITOR')")
    public List<UserResponse> getUsers(UserRole role) {
        List<User> userList = (role == null)
                ? userRepository.findAllWithProfile()
                : userRepository.findByRoleWithProfile(role);

        return userList.stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id))
            throw new UserNotFoundException(id);

        refreshTokenRepository.deleteAllByUserId(id);
        userRepository.deleteById(id);
    }

    private AuthResult toAuthResult(User user) {
        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());

        String jti = UUID.randomUUID().toString();
        String accessToken = jwtService.generateAccessToken(details);
        String refreshToken = jwtService.generateRefreshToken(details, jti);

        Instant expiresAt = Instant.now().plusSeconds(jwtService.getRefreshExpirationInSeconds());
        RefreshToken tokenEntity = new RefreshToken(jti, user.getId(), expiresAt);
        refreshTokenRepository.save(tokenEntity);

        long expiresIn = jwtService.getAccessExpirationInSeconds();

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getRole(),
                accessToken,
                expiresIn
        );

        return new AuthResult(response, refreshToken);
    }
}