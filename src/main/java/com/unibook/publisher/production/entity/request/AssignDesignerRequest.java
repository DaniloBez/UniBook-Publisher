package com.unibook.publisher.production.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to assign a cover designer to a manuscript")
public record AssignDesignerRequest(
      @Schema(description = "ID of the user with the DESIGNER role", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
      @NotNull
      @JsonProperty("designer_id")
      UUID designerId
) {}
