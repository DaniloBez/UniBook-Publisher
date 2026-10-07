package com.unibook.publisher.production.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to post a message in a feedback thread")
public record ThreadMessageRequest(
        @Schema(description = "Message text", example = "Thanks, I will fix this in the next revision")
        @NotBlank
        String content
) {}
