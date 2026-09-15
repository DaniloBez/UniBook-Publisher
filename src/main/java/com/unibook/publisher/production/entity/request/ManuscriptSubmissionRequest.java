package com.unibook.publisher.production.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ManuscriptSubmissionRequest(
      @NotBlank
      String title,

      @NotBlank
      String annotation,

      @NotBlank
      @JsonProperty("draft_file_url")
      String draftFileUrl,

      @NotEmpty
      @JsonProperty("genre_ids")
      List<UUID> genreIds
){ }
