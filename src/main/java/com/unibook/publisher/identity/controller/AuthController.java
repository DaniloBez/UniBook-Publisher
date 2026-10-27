package com.unibook.publisher.identity.controller;

import com.unibook.publisher.common.exception.security.InvalidCredentialsException;
import com.unibook.publisher.common.security.jwt.JwtService;
import com.unibook.publisher.identity.entity.request.LoginRequest;
import com.unibook.publisher.identity.entity.request.RegisterRequest;
import com.unibook.publisher.identity.entity.response.AuthResponse;
import com.unibook.publisher.identity.entity.response.AuthResult;
import com.unibook.publisher.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration and login")
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;
    private final boolean cookieSecure;

    public AuthController(
            UserService userService,
            JwtService jwtService,
            @Value("${security.jwt.cookie.secure:false}") boolean cookieSecure
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new author", description = "Creates a user with the AUTHOR role and a profile. The Location header points to the new user's profile")
    @ApiResponse(responseCode = "201", description = "User successfully registered")
    @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A user with this email already exists", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AuthResponse> register(
            @RequestBody @Valid RegisterRequest request,
            HttpServletResponse httpResponse
    ) {
        AuthResult result = userService.register(request);
        setRefreshTokenCookie(httpResponse, result.refreshToken());

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/users/{id}/profile")
                .buildAndExpand(result.response().userId())
                .toUri();

        return ResponseEntity.created(location).body(result.response());
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticates a user by email and password and returns the user ID, role and access token")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Wrong email or password", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse httpResponse
    ) {
        AuthResult result = userService.login(request);
        setRefreshTokenCookie(httpResponse, result.refreshToken());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generates new pair of access and fresh tokens")
    @ApiResponse(responseCode = "200", description = "Access token successfully refreshed")
    @ApiResponse(responseCode = "400", description = "Request validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse
    ) {
        if (refreshToken == null)
            throw new InvalidCredentialsException("Refresh token відсутній у cookie");

        AuthResult result = userService.refreshToken(refreshToken);
        setRefreshTokenCookie(httpResponse, result.refreshToken());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Completes the user logout process")
    @ApiResponse(responseCode = "204", description = "User successfully logged out")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken, // <-- Зчитуємо cookie
            HttpServletResponse httpResponse
    ) {
        userService.logout(refreshToken);
        clearRefreshTokenCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(jwtService.getRefreshExpirationInSeconds())
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}