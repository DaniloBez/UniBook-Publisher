package com.unibook.publisher.production.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "chapters",
        indexes = @Index(name = "idx_chapters_manuscript_id", columnList = "manuscript_id")
)
@NoArgsConstructor
@Getter
@Setter
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chapter_id")
    private UUID chapterId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manuscript_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Manuscript manuscript;

    @Column(name = "chapter_title", nullable = false)
    private String chapterTitle;

    @Column(name = "chapter_index", nullable = false)
    private int chapterIndex; //порядковий номер розділу

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber ASC")
    private List<Revision> revisions = new ArrayList<>();

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedbackThread> threads = new ArrayList<>();

    public Chapter(Manuscript manuscript, String chapterTitle, int chapterIndex) {
        this.manuscript = manuscript;
        this.chapterTitle = chapterTitle;
        this.chapterIndex = chapterIndex;
    }

    public UUID getManuscriptId() {
        return manuscript.getManuscriptId();
    }

    public void addRevision(Revision revision) {
        revisions.add(revision);
        revision.setChapter(this);
    }

    public void addThread(FeedbackThread thread) {
        threads.add(thread);
        thread.setChapter(this);
    }
}
