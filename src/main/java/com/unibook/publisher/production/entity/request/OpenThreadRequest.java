package com.unibook.publisher.production.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Schema(description = "Request to open a feedback thread on a chapter, optionally anchored to a quote and carrying a text replacement suggestion")
public record OpenThreadRequest(
        @Schema(description = "Text of the first message in the thread", example = "This sentence is hard to read, consider rephrasing")
        @NotBlank
        String initialMessage,

        @Schema(description = "Proposed replacement text; if provided, the thread becomes a suggestion with PENDING status", example = "The old lighthouse stood silent above the sea.", nullable = true)
        String suggestedText,

        @Schema(description = "ID of the revision the quote refers to; required to validate the quote", example = "6f1e3d5a-9c2b-4e7f-8a0d-1b3c5e7f9a2c", nullable = true)
        UUID targetRevisionId,

        @Schema(description = "Exact fragment of the revision text being discussed", example = "The lighthouse was old and it was standing quietly", nullable = true)
        String quotedText,

        @Schema(description = "Start offset (inclusive, zero-based) of the quote in the revision text", example = "120", nullable = true)
        Integer positionFrom,

        @Schema(description = "End offset (exclusive) of the quote in the revision text", example = "170", nullable = true)
        Integer positionTo
) {}
