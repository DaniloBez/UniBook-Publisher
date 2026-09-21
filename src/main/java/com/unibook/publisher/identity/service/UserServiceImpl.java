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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email()))
            throw new EmailAlreadyExistsException(request.email());

        User user = userRepository.save(new User(
                null,
                request.email(),
                passwordEncoder.encode(request.password()),
                UserRole.AUTHOR
        ));

        userProfileRepository.save(new UserProfile(
                user.id(),
                request.displayName(),
                request.bio(),
                request.avatarUrl(),
                request.preferredLocale()
        ));

        return new AuthResponse(user.id(), user.role(), "jwt-token-for-" + user.id());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.getByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.hashedPassword()))
            throw new InvalidCredentialsException();

        return new AuthResponse(user.id(), user.role(), "jwt-token-for-" + user.id());
    }

    @Override
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.get(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        UserProfile profile = userProfileRepository.get(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserResponse.from(user, profile);
    }

    @Override
    public UserResponse updateUserProfile(UUID userId, UserProfileUpdateRequest request) {
        User user = userRepository.get(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        UserProfile profile = userProfileRepository.update(
                userId,
                new UserProfile(
                        userId,
                        request.displayName(),
                        request.bio(),
                        request.avatarUrl(),
                        request.preferredLocale()
                )
        ).orElseThrow(() -> new UserNotFoundException(userId));

        return UserResponse.from(user, profile);
    }

    @Override
    public UserResponse createStaff(StaffRequest request) {
        if (userRepository.existsByEmail(request.email()))
            throw new EmailAlreadyExistsException(request.email());

        User user = userRepository.save(new User(
                null,
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role()
        ));

        UserProfile profile = userProfileRepository.save(new UserProfile(
                user.id(),
                request.displayName(),
                request.bio(),
                request.avatarUrl(),
                request.preferredLocale()
        ));

        return UserResponse.from(user, profile);
    }

    @Override
    public List<UserResponse> getUsers(UserRole role) {
        List<User> userList = (role == null)
                ? userRepository.findAll()
                : userRepository.findByRole(role);

        return userList.stream()
                .map(user -> UserResponse.from(user, userProfileRepository.get(user.id()).orElse(null)))
                .toList();
    }
}
