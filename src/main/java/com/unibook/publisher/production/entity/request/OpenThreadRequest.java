package com.unibook.publisher.production.entity.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record OpenThreadRequest(
        @NotBlank
        String initialMessage,

        String suggestedText,
        UUID targetRevisionId,
        String quotedText,
        Integer positionFrom,
        Integer positionTo
) {}
