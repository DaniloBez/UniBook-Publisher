package com.unibook.publisher.communication.service;

import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.communication.entity.response.UnreadCountResponse;

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

    List<NotificationResponse> getUserNotifications(AuthenticatedUser currentUser, Boolean unreadOnly);

    NotificationResponse markAsRead(UUID notificationId, AuthenticatedUser currentUser);

    UnreadCountResponse getUnreadCount(UUID userId);

    void markAllAsRead(UUID userId);
}
