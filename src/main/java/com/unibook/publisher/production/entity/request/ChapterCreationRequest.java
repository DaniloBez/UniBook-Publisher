package com.unibook.publisher.production.entity.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ChapterCreationRequest(
        @NotBlank
        String chapterTitle,

        @Min(value = 1)
        int chapterIndex
) {}
