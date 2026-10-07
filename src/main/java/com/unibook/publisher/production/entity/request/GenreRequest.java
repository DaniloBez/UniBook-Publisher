package com.unibook.publisher.production.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to create or rename a genre")
public record GenreRequest(
        @Schema(description = "Unique genre name", example = "Science fiction")
        @NotBlank
        @JsonProperty("genreName")
        String genreName
) {}
