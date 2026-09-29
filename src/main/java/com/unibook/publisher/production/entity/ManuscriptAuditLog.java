package com.unibook.publisher.production.entity;

import com.unibook.publisher.production.enums.ManuscriptStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "manuscript_audit_logs")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ManuscriptAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", nullable = false)
    private Manuscript manuscript;

    @Column(name = "changed_by_user_id", nullable = false)
    private UUID changedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private ManuscriptStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private ManuscriptStatus newStatus;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
