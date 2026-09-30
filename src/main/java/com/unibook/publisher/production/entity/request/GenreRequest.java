package com.unibook.publisher.production.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record GenreRequest(
        @NotBlank
        @JsonProperty("genreName")
        String genreName
) {}
