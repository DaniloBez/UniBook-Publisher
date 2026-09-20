package com.unibook.publisher.communication.repository;

import com.unibook.publisher.communication.entity.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    List<Notification> findByRecipientId(UUID recipientId);
}
