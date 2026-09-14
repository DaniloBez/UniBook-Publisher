package com.unibook.publisher.identity.controller;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.exception.ForbiddenActionException;
import com.unibook.publisher.identity.entity.request.StaffRequest;
import com.unibook.publisher.identity.entity.response.UserResponse;
import com.unibook.publisher.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/staff")
    public ResponseEntity<UserResponse> createStaff(
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
}
