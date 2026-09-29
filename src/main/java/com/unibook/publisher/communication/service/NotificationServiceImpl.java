package com.unibook.publisher.communication.service;

import com.unibook.publisher.common.exception.notfound.NotificationNotFoundException;
import com.unibook.publisher.common.exception.security.ForbiddenActionException;
import com.unibook.publisher.common.logging.AppLogger;
import com.unibook.publisher.communication.entity.Notification;
import com.unibook.publisher.communication.entity.NotificationType;
import com.unibook.publisher.communication.entity.response.NotificationResponse;
import com.unibook.publisher.communication.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final AppLogger logger;

    public NotificationServiceImpl(NotificationRepository notificationRepository, AppLogger logger) {
        this.notificationRepository = notificationRepository;
        this.logger = logger;
    }

    @Override
    @Transactional
    public NotificationResponse send(
            UUID recipientId,
            UUID senderId,
            UUID targetId,
            String title,
            String message,
            NotificationType type
    ) {
        Notification notification = new Notification(
                recipientId,
                senderId,
                targetId,
                title,
                message,
                type
        );

        Notification saved = notificationRepository.save(notification);

        logger.info(
            "Created notification {} for recipient {} from sender {}",
            saved.getId(),
            recipientId,
            senderId
        );

        return NotificationResponse.from(saved);
    }

    @Override
    public List<NotificationResponse> getUserNotifications(UUID recipientId, Boolean unreadOnly) {
        List<Notification> list = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientId)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);

        return list.stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getRecipientId().equals(userId))
            throw new ForbiddenActionException("Можна відмічати тільки свої повідомлення");

        notification.markAsRead();
        Notification updated = notificationRepository.save(notification);

        logger.info(
            "Updated notification {}: marked as read by user {}",
            notificationId,
            userId
        );

        return NotificationResponse.from(updated);
    }
}
