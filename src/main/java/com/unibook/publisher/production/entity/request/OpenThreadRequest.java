package com.unibook.publisher.production.entity.request;

import jakarta.validation.constraints.NotBlank;

public record OpenThreadRequest(
        @NotBlank
        String initialMessage,

        String suggestedText
) {}
