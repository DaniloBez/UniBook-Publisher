package com.unibook.publisher.identity.controller;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.identity.entity.request.UserProfileUpdateRequest;
import com.unibook.publisher.identity.entity.response.UserResponse;
import com.unibook.publisher.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profiles and user directory")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get user profile", description = "Returns the account data and profile of the user")
        @ApiResponse(responseCode = "200", description = "Profile found")
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<UserResponse> getUserProfile(
            @Parameter(description = "User ID", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Update own profile", description = "Replaces the profile fields of the user. A user can edit only their own profile")
        @ApiResponse(responseCode = "200", description = "Profile successfully updated")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "Attempt to edit the profile of another user", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<UserResponse> updateUserProfile(
            @Parameter(description = "ID of the user making the request (temporary replacement for JWT)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID userId, // Поки не використовуємо jwt, отримуємо дані з хедера
            @Parameter(description = "ID of the user whose profile is updated; must match X-User-Id", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,
            @RequestBody @Valid UserProfileUpdateRequest request
    ) {
        if (!userId.equals(id))
            throw new ForbiddenActionException("Користувач має право редагувати тільки свій профіль");

        return ResponseEntity.ok(userService.updateUserProfile(userId, request));
    }

    @GetMapping
    @Operation(summary = "Get users", description = "Returns all users, optionally filtered by role. Chief editor or administrator only")
        @ApiResponse(responseCode = "200", description = "Users successfully retrieved")
        @ApiResponse(responseCode = "403", description = "Only a chief editor or an administrator can list users", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<UserResponse>> getUsers(
            @Parameter(description = "Role of the user making the request", required = true, example = "CHIEF_EDITOR")
            @RequestHeader("X-User-Role") UserRole callerRole,
            @Parameter(description = "Filter: return only users with this role", example = "EDITOR")
            @RequestParam(required = false) UserRole role
    ) {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.CHIEF_EDITOR)
            throw new ForbiddenActionException("Переглядати список користувачів можуть лише головний редактор або адміністратор");

        return ResponseEntity.ok(userService.getUsers(role));
    }
}
