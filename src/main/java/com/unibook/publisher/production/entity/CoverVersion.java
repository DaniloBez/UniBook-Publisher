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
        name = "cover_versions",
        indexes = @Index(name = "idx_cover_versions_manuscript_id", columnList = "manuscript_id")
)
@NoArgsConstructor
@Getter
@Setter
public class CoverVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manuscript_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Manuscript manuscript;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public CoverVersion(Manuscript manuscript, String fileUrl, UUID uploadedByUserId, int versionNumber, Instant uploadedAt) {
        this.manuscript = manuscript;
        this.fileUrl = fileUrl;
        this.uploadedByUserId = uploadedByUserId;
        this.versionNumber = versionNumber;
        this.uploadedAt = uploadedAt;
    }

    public UUID getManuscriptId() {
        return manuscript.getManuscriptId();
    }
}
