package com.unibook.publisher.production.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record ManuscriptUpdateRequest(
        @NotBlank
        String title,

        @NotBlank
        String annotation,

        @NotBlank
        String draftFileUrl,

        @NotEmpty
        Set<UUID> genreIds
) {}