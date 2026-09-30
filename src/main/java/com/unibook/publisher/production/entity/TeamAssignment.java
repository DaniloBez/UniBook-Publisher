package com.unibook.publisher.production.entity;

import com.unibook.publisher.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "team_assignments")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TeamAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assignment_id")
    private UUID assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Manuscript manuscript;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role; //дизайнер або редактор

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
}
