package com.unibook.publisher.identity.controller;

import com.unibook.publisher.identity.entity.request.LoginRequest;
import com.unibook.publisher.identity.entity.request.RegisterRequest;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration and login")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new author", description = "Creates a user with the AUTHOR role and a profile. The Location header points to the new user's profile")
        @ApiResponse(responseCode = "201", description = "User successfully registered")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "409", description = "A user with this email already exists", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        AuthResponse response = userService.register(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/users/{id}/profile")
                .buildAndExpand(response.userId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticates a user by email and password and returns the user ID, role and access token")
        @ApiResponse(responseCode = "200", description = "Authentication successful")
        @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "401", description = "Wrong email or password", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
}
