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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getRole(),
                "jwt-token-for-" + savedUser.getId());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.getByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getHashedPassword()))
            throw new InvalidCredentialsException();

        return new AuthResponse(
                user.getId(),
                user.getRole(),
                "jwt-token-for-" + user.getId()
        );
    }

    @Override
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(UUID userId, UserProfileUpdateRequest request) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

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
    public List<UserResponse> getUsers(UserRole role) {
        List<User> userList = (role == null)
                ? userRepository.findAllWithProfile()
                : userRepository.findByRoleWithProfile(role);

        return userList.stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id))
            throw new UserNotFoundException(id);

        userRepository.deleteById(id);
    }
}
