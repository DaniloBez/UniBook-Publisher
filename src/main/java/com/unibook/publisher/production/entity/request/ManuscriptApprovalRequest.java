package com.unibook.publisher.production.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to approve a manuscript and assign an editor to it")
public record ManuscriptApprovalRequest(
      @Schema(description = "ID of the user with the EDITOR role who will work on the manuscript", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
      @NotNull
      @JsonProperty("editor_id")
      UUID editorId
) {}
