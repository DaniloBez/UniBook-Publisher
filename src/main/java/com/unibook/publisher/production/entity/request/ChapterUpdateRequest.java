package com.unibook.publisher.production.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update the title and position of a chapter")
public record ChapterUpdateRequest(
        @Schema(description = "Chapter title", example = "Chapter 1. The New Beginning")
        @NotBlank
        String chapterTitle,

        @Schema(description = "Position of the chapter in the manuscript, starting from 1", example = "1", minimum = "1")
        @Min(value = 1)
        int chapterIndex
) {}
