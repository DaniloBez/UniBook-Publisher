package com.unibook.publisher.communication.service;

import com.unibook.publisher.common.exception.notfound.NotificationNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.communication.entity.Notification;
import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.communication.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final AppLogger logger;

    public NotificationServiceImpl(NotificationRepository notificationRepository, AppLogger logger) {
        this.notificationRepository = notificationRepository;
        this.logger = logger;
    }

    @Override
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

        logger.info(
            "Created notification {} for recipient {} from sender {}",
            saved.id(),
            recipientId,
            senderId
        );

        return NotificationResponse.from(saved);
    }

    @Override
    public List<NotificationResponse> getUserNotifications(UUID recipientId, Boolean unreadOnly) {
        return notificationRepository.findByRecipientId(recipientId).stream()
                .filter(n -> unreadOnly == null || !unreadOnly || !n.isRead())
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.recipientId().equals(userId))
            throw new ForbiddenActionException("Можна відмічати тільки свої повідомлення");

        Notification updated = notificationRepository.save(notification.markAsRead());

        logger.info(
            "Updated notification {}: marked as read by user {}",
            notificationId,
            userId
        );

        return NotificationResponse.from(updated);
    }
}
