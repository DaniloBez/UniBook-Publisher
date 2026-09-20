package com.unibook.publisher.communication.repository;

import com.unibook.publisher.communication.entity.Notification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryNotificationRepository implements NotificationRepository {
    private final Map<UUID, Notification> notifications = new ConcurrentHashMap<>();

    @Override
    public Notification save(Notification notification) {
        UUID id = notification.id() != null ? notification.id() : UUID.randomUUID();
        Notification toSave = new Notification(
                id,
                notification.recipientId(),
                notification.senderId(),
                notification.targetId(),
                notification.title(),
                notification.message(),
                notification.type(),
                notification.isRead(),
                notification.createdAt() != null ? notification.createdAt() : Instant.now()
        );
        notifications.put(id, toSave);
        return toSave;
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return Optional.ofNullable(notifications.get(id));
    }

    @Override
    public List<Notification> findByRecipientId(UUID recipientId) {
        return notifications.values().stream()
                .filter(n -> n.recipientId().equals(recipientId))
                .sorted(Comparator.comparing(Notification::createdAt).reversed())
                .toList();
    }
}
