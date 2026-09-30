package com.unibook.publisher.production.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "thread_messages",
        indexes = @Index(name = "idx_thread_messages_thread_id", columnList = "thread_id")
)
@NoArgsConstructor
@Getter
@Setter
public class ThreadMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FeedbackThread thread;

    @Column(name = "sender_user_id", nullable = false)
    private UUID senderUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    public ThreadMessage(UUID senderUserId, String content, Instant sentAt) {
        this.senderUserId = senderUserId;
        this.content = content;
        this.sentAt = sentAt;
    }

    public UUID getThreadId() {
        return thread.getId();
    }
}
