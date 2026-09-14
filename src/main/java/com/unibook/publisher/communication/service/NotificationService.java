package com.unibook.publisher.communication.service;

import com.unibook.publisher.common.exception.ForbiddenActionException;
import com.unibook.publisher.common.exception.ResourceNotFoundException;
import com.unibook.publisher.communication.entity.Notification;
import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.communication.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationResponse send(
            UUID recipientId,
            UUID senderId,
            UUID targetId,
            String title,
            String message,
            NotificationType type
    ) {
        Notification notification = new Notification(
                null,
                recipientId,
                senderId,
                targetId,
                title,
                message,
                type,
                false,
                Instant.now()
        );
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.from(saved);
    }

    public NotificationResponse send(UUID recipientId, String title, String message, NotificationType type) {
        return send(recipientId, null, null, title, message, type);
    }

    public List<NotificationResponse> getUserNotifications(UUID recipientId, Boolean unreadOnly) {
        return notificationRepository.findByRecipientId(recipientId).stream()
                .filter(n -> unreadOnly == null || !unreadOnly || !n.isRead())
                .map(NotificationResponse::from)
                .toList();
    }

    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Сповіщення не знайдено"));

        if (!notification.recipientId().equals(userId))
            throw new ForbiddenActionException("Можна відмічати тільки свої повідомлення");

        Notification updated = notificationRepository.save(notification.markAsRead());
        return NotificationResponse.from(updated);
    }
}
