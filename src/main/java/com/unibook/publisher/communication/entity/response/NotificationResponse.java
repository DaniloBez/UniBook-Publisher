package com.unibook.publisher.communication.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.communication.entity.Notification;
import com.unibook.publisher.communication.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,

        @JsonProperty("sender_id")
        UUID senderId,

        @JsonProperty("target_id")
        UUID targetId,

        String title,

        String message,

        NotificationType type,

        @JsonProperty("is_read")
        boolean isRead,

        @JsonProperty("created_at")
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getSenderId(),
                notification.getTargetId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
