package com.unibook.publisher.production.entity;

import com.unibook.publisher.production.enums.SuggestionStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "feedback_threads",
        indexes = @Index(name = "idx_feedback_threads_chapter_id", columnList = "chapter_id")
)
@NoArgsConstructor
@Getter
@Setter
public class FeedbackThread {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Chapter chapter;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreadStatus status;

    @Column(name = "is_suggestion", nullable = false)
    private boolean isSuggestion;

    @Column(name = "suggested_text", columnDefinition = "TEXT")
    private String suggestedText;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggestion_status")
    private SuggestionStatus suggestionStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "target_revision_id")
    private UUID targetRevisionId;

    @Column(name = "quoted_text", columnDefinition = "TEXT")
    private String quotedText;

    @Column(name = "position_from")
    private Integer positionFrom;

    @Column(name = "position_to")
    private Integer positionTo;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    private List<ThreadMessage> messages = new ArrayList<>();

    public FeedbackThread(
            UUID createdByUserId,
            String suggestedText,
            UUID targetRevisionId,
            String quotedText,
            Integer positionFrom,
            Integer positionTo
    ) {
        this.createdByUserId = createdByUserId;
        this.status = ThreadStatus.OPEN;
        this.createdAt = Instant.now();

        boolean hasSuggestion = suggestedText != null && !suggestedText.isBlank();
        this.isSuggestion = hasSuggestion;
        this.suggestedText = hasSuggestion ? suggestedText : null;
        this.suggestionStatus = hasSuggestion ? SuggestionStatus.PENDING : null;

        this.targetRevisionId = targetRevisionId;
        this.quotedText = quotedText;
        this.positionFrom = positionFrom;
        this.positionTo = positionTo;
    }

    public UUID getChapterId() {
        return chapter.getChapterId();
    }

    public void addMessage(ThreadMessage message) {
        messages.add(message);
        message.setThread(this);
    }
}
