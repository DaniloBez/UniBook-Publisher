package com.unibook.publisher.communication.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Count of unread notifications")
public record UnreadCountResponse(
        @JsonProperty("unread_count")
        long unreadCount
) {}
