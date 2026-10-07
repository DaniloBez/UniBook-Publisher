package com.unibook.publisher.production.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Request to replace the editable fields of a manuscript")
public record ManuscriptUpdateRequest(
        @Schema(description = "Manuscript title", example = "The Last Lighthouse")
        @NotBlank
        String title,

        @Schema(description = "Short annotation describing the manuscript", example = "A novel about the last keeper of a lighthouse on the Black Sea coast")
        @NotBlank
        String annotation,

        @Schema(description = "URL of the uploaded draft file", example = "https://storage.example.com/drafts/the-last-lighthouse-v2.docx")
        @NotBlank
        String draftFileUrl,

        @Schema(description = "IDs of the genres the manuscript belongs to; at least one is required", example = "[\"5e8f2a4c-1b3d-4c6e-9f7a-0d2b4c6e8f1a\"]")
        @NotEmpty
        Set<UUID> genreIds
) {}
