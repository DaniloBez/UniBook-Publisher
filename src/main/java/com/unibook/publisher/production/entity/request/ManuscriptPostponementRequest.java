package com.unibook.publisher.production.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to postpone a manuscript")
public record ManuscriptPostponementRequest(
      @Schema(description = "Comment for the author explaining the postponement", example = "The publishing plan for this quarter is full; we will revisit in spring")
      @NotBlank
      String comment
){}
