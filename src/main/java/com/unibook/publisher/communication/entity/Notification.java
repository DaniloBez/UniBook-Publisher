package com.unibook.publisher.communication.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id, is_read")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "recipient_id")
    private UUID recipientId;

    @Column(name = "sender_id")
    private UUID senderId;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2048)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationType type;

    @Column(nullable = false, name = "is_read")
    private boolean isRead = false;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    public Notification(
            UUID recipientId,
            UUID senderId,
            UUID targetId,
            String title,
            String message,
            NotificationType type
    ) {
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.targetId = targetId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.createdAt = Instant.now();
    }
    
    public void markAsRead() {
        this.isRead = true;
    }
}
