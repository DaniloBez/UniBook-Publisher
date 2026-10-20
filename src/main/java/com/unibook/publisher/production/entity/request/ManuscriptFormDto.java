
package com.unibook.publisher.production.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Form data for submitting a new manuscript")
public record ManuscriptFormDto(
      @Schema(description = "Manuscript title", example = "The Last Lighthouse")
      @NotBlank
      @Size(min = 2, max = 150)
      String title,

      @Schema(description = "Short annotation describing the manuscript",
              example = "A novel about the last keeper of a lighthouse on the Black Sea coast")
      @NotBlank
      @Size(min = 10, max = 2000)
      String annotation,

      @Schema(description = "URL of the uploaded draft file",
              example = "https://storage.example.com/drafts/the-last-lighthouse.docx")
      @NotBlank
      @JsonProperty("draft_file_url")
      String draftFileUrl,

      @Schema(description = "IDs of the manuscript genres; at least one is required",
              example = "[\"5e8f2a4c-1b3d-4c6e-9f7a-0d2b4c6e8f1a\"]")
      @NotEmpty(message = "Оберіть хоча б один жанр")
      @JsonProperty("genre_ids")
      List<UUID> genreIds
) { }

