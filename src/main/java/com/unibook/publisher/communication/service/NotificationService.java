package com.unibook.publisher.communication.service;

import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.entity.response.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse send(
            UUID recipientId,
            UUID senderId,
            UUID targetId,
            String title,
            String message,
            NotificationType type
    );

    List<NotificationResponse> getUserNotifications(UUID recipientId, Boolean unreadOnly);

    NotificationResponse markAsRead(UUID notificationId, UUID userId);
}
