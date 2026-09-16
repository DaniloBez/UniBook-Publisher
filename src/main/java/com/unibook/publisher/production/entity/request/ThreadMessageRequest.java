package com.unibook.publisher.production.entity.request;

import jakarta.validation.constraints.NotBlank;

public record ThreadMessageRequest(
        @NotBlank
        String content
) {}
