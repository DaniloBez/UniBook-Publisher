package com.unibook.publisher.production.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to reject a manuscript")
public record ManuscriptRejectionRequest(
      @Schema(description = "Reason for the rejection, sent to the author", example = "The manuscript does not match the publisher's profile")
      @NotBlank
      String reason
) {}
