package com.unibook.publisher.identity.service;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.identity.entity.User;
import com.unibook.publisher.identity.entity.request.*;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.identity.entity.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    UserResponse getUserProfile(UUID userId);

    UserResponse updateUserProfile(UUID profileUserId, User currentUser, UserProfileUpdateRequest request);

    UserResponse createStaff(StaffRequest request);

    List<UserResponse> getUsers(UserRole role);

    void deleteUser(UUID id);
}
