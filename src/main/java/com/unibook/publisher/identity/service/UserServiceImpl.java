package com.unibook.publisher.identity.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.conflict.EmailAlreadyExistsException;
import com.unibook.publisher.common.exception.notfound.UserNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.exception.security.InvalidCredentialsException;
import com.unibook.publisher.common.security.jwt.JwtService;
import com.unibook.publisher.identity.entity.User;
import com.unibook.publisher.identity.entity.UserProfile;
import com.unibook.publisher.identity.entity.request.*;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.identity.entity.response.UserResponse;
import com.unibook.publisher.identity.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
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

        return toAuthResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.getByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getHashedPassword()))
            throw new InvalidCredentialsException();

        return toAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.refreshToken();
        if(!jwtService.isRefreshToken(token)) {
            throw new InvalidCredentialsException("Недійсний або протермінований refresh token");
        }
        String email = jwtService.extractUsername(token);
        User user = userRepository.getByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Недійсний або протермінований refresh token"));

        return toAuthResponse(user);
    }

    @Override
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(UUID profileUserId, User currentUser, UserProfileUpdateRequest request) {
        if (!currentUser.getId().equals(profileUserId))
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

        userRepository.deleteById(id);
    }

    private AuthResponse toAuthResponse(User user) {
        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtService.generateAccessToken(details);
        String refreshToken = jwtService.generateRefreshToken(details);
        long expiresIn = jwtService.getAccessExpirationInSeconds();

        return new AuthResponse(
                user.getId(),
                user.getRole(),
                accessToken,
                refreshToken,
                expiresIn
        );
    }
}
