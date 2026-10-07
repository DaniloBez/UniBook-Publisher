package com.unibook.publisher.identity.controller;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.identity.entity.request.StaffRequest;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration", description = "Administrative operations: staff accounts and user removal")
public class AdminController {
    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/staff")
    @Operation(summary = "Create a staff account", description = "Creates a user with a staff role (editor, designer, accountant, etc.). Administrator only. The Location header points to the new user's profile")
        @ApiResponse(responseCode = "201", description = "Staff account successfully created")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "403", description = "Only an administrator can create staff accounts", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "409", description = "A user with this email already exists", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<UserResponse> createStaff(
            @Parameter(description = "Role of the user making the request", required = true, example = "ADMIN")
            @RequestHeader("X-User-Role") UserRole role,
            @RequestBody @Valid StaffRequest request
    ) {
        if (role != UserRole.ADMIN)
            throw new ForbiddenActionException("Створювати робітників може тільки адміністратор");

        UserResponse response = userService.createStaff(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/users/{id}/profile")
                .buildAndExpand(response.userId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete a user", description = "Permanently deletes a user account. Administrator only")
        @ApiResponse(responseCode = "204", description = "User successfully deleted")
        @ApiResponse(responseCode = "403", description = "Only an administrator can delete users", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "Role of the user making the request", required = true, example = "ADMIN")
            @RequestHeader("X-User-Role") UserRole role,
            @Parameter(description = "ID of the user to delete", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id
    ) {
        if (role != UserRole.ADMIN)
            throw new ForbiddenActionException("Видаляти користувачів може тільки адміністратор");

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
