package com.unibook.publisher.identity.entity.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Email
        @NotBlank
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
