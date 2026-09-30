package com.unibook.publisher.production.entity;

import com.unibook.publisher.production.enums.ManuscriptStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "manuscripts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Manuscript {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "manuscript_id")
    private UUID manuscriptId;

    @Column(nullable = false)
    private String title;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManuscriptStatus status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "manuscript_genres",
            joinColumns = @JoinColumn(name = "manuscript_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @Column(nullable = false)
    private String annotation;

    @Column(name = "draft_file_url", nullable = false)
    private String draftFileUrl; //посилання на файл з чернеткою рукопису

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
}
