package com.unibook.publisher.production.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ManuscriptApprovalRequest(
      @NotNull
      @JsonProperty("editor_id")
      UUID editorId
) {}
