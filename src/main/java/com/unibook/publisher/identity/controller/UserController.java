package com.unibook.publisher.identity.controller;

import com.unibook.publisher.common.exception.ForbiddenActionException;
import com.unibook.publisher.identity.entity.request.UserProfileUpdateRequest;
import com.unibook.publisher.identity.entity.response.UserResponse;
import com.unibook.publisher.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<UserResponse> updateUserProfile(
            @RequestHeader("X-User-Id") UUID userId, // Поки не використовуємо jwt, отримуємо дані з хедера
            @PathVariable UUID id,
            @RequestBody @Valid UserProfileUpdateRequest request
    ) {
        if (!userId.equals(id))
            throw new ForbiddenActionException("Користувач має право редагувати тільки свій профіль");

        return ResponseEntity.ok(userService.updateUserProfile(userId, request));
    }
}
