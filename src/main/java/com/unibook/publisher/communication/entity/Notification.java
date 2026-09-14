package com.unibook.publisher.communication.entity;

import java.time.Instant;
import java.util.UUID;

public record Notification(
        UUID id,
        UUID recipientId,
        UUID senderId,
        UUID targetId,
        String title,
        String message,
        NotificationType type,
        boolean isRead,
        Instant createdAt
) {
    public Notification markAsRead() {
        return new Notification(id, recipientId, senderId, targetId, title, message, type, true, createdAt);
    }
}
