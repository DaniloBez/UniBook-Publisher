package com.unibook.publisher.production.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Query parameters selecting two revisions of a chapter to compare")
public record DiffRequest(
        @Schema(description = "ID of the base (older) revision", example = "2b7a9c4e-8d1f-4a3b-b5c6-7e8f9a0b1c2d")
        @NotNull
        UUID fromRevisionId,

        @Schema(description = "ID of the revision to compare against the base (newer)", example = "6f1e3d5a-9c2b-4e7f-8a0d-1b3c5e7f9a2c")
        @NotNull
        UUID toRevisionId
) {}
