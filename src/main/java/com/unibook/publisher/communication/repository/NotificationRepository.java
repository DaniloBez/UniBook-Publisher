package com.unibook.publisher.communication.repository;

import com.unibook.publisher.communication.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID id);

    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(UUID recipientId);
}
