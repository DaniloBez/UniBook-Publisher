package com.unibook.publisher.production.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignDesignerRequest(
      @NotNull
      @JsonProperty("designer_id")
      UUID designerId
) {}
