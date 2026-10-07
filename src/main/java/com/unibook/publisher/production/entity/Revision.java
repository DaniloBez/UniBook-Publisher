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
        name = "revisions",
        indexes = @Index(name = "idx_revisions_chapter_id", columnList = "chapter_id")
)
@NoArgsConstructor
@Getter
@Setter
public class Revision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "revision_id")
    private UUID revisionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Chapter chapter;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public Revision(int versionNumber, String fileUrl, UUID uploadedByUserId, Instant uploadedAt) {
        this.versionNumber = versionNumber;
        this.fileUrl = fileUrl;
        this.uploadedByUserId = uploadedByUserId;
        this.uploadedAt = uploadedAt;
    }

    public UUID getChapterId() {
        return chapter.getChapterId();
    }
}
