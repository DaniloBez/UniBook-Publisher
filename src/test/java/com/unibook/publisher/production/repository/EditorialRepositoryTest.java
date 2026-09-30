package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.*;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.enums.ThreadStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EditorialRepositoryTest {
    @Autowired private ManuscriptRepository manuscriptRepository;
    @Autowired private ChapterRepository chapterRepository;
    @Autowired private RevisionRepository revisionRepository;
    @Autowired private FeedbackThreadRepository threadRepository;
    @Autowired private ThreadMessageRepository messageRepository;
    @Autowired private CoverVersionRepository coverVersionRepository;
    @Autowired private EntityManager em;

    private Manuscript manuscript;

    @BeforeEach
    void setUp() {
        manuscript = new Manuscript();
        manuscript.setTitle("Книга");
        manuscript.setAuthorId(UUID.randomUUID());
        manuscript.setStatus(ManuscriptStatus.IN_PROGRESS);
        manuscript.setAnnotation("анотація");
        manuscript.setDraftFileUrl("url");
        manuscript.setSubmittedAt(Instant.now());
        manuscript = manuscriptRepository.save(manuscript);
    }

    private Chapter chapterWithRevisions(String title, int index, int revisions) {
        Chapter chapter = new Chapter(manuscript, title, index);
        for (int v = 1; v <= revisions; v++) {
            chapter.addRevision(new Revision(v, "url-" + v, UUID.randomUUID(), Instant.now(), "текст " + v));
        }
        return chapterRepository.save(chapter);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void findByManuscriptIdWithRevisions_returnsOrderedChaptersWithRevisionsLoaded() {
        chapterWithRevisions("Другий", 2, 1);
        chapterWithRevisions("Перший", 1, 3);
        flushAndClear();

        List<Chapter> chapters = chapterRepository.findByManuscriptIdWithRevisions(manuscript.getManuscriptId());

        assertEquals(2, chapters.size());
        assertEquals("Перший", chapters.get(0).getChapterTitle());
        assertEquals(3, chapters.get(0).getRevisions().size());
        assertEquals(manuscript.getManuscriptId(), chapters.get(0).getManuscriptId());
    }

    @Test
    void deleteChapter_cascadesToRevisionsAndThreadsWithMessages() {
        Chapter chapter = chapterWithRevisions("Розділ", 1, 2);
        FeedbackThread thread = newThread();
        thread.addMessage(new ThreadMessage(UUID.randomUUID(), "привіт", Instant.now()));
        chapter.addThread(thread);
        chapterRepository.save(chapter);
        flushAndClear();
        assertEquals(2, revisionRepository.count());
        assertEquals(1, messageRepository.count());

        chapterRepository.delete(chapterRepository.findById(chapter.getChapterId()).orElseThrow());
        flushAndClear();

        assertEquals(0, revisionRepository.count());
        assertEquals(0, threadRepository.count());
        assertEquals(0, messageRepository.count());
    }

    @Test
    void deleteThread_cascadesToMessages() {
        Chapter chapter = chapterWithRevisions("Розділ", 1, 1);
        FeedbackThread thread = newThread();
        thread.addMessage(new ThreadMessage(UUID.randomUUID(), "перше", Instant.now()));
        thread.addMessage(new ThreadMessage(UUID.randomUUID(), "друге", Instant.now()));
        chapter.addThread(thread);
        chapterRepository.save(chapter);
        flushAndClear();

        List<FeedbackThread> loaded = threadRepository.findByChapterIdWithMessages(chapter.getChapterId());
        assertEquals(1, loaded.size());
        assertEquals(2, loaded.get(0).getMessages().size());

        threadRepository.delete(loaded.get(0));
        flushAndClear();

        assertEquals(0, messageRepository.count());
        assertEquals(1, chapterRepository.count());
    }

    @Test
    void findByChapterIdAndStatusWithMessages_filtersByStatus() {
        Chapter chapter = chapterWithRevisions("Розділ", 1, 1);
        FeedbackThread open = newThread();
        FeedbackThread resolved = newThread();
        resolved.setStatus(ThreadStatus.RESOLVED);
        chapter.addThread(open);
        chapter.addThread(resolved);
        chapterRepository.save(chapter);
        flushAndClear();

        assertEquals(1, threadRepository.findByChapterIdAndStatusWithMessages(chapter.getChapterId(), ThreadStatus.OPEN).size());
        assertEquals(1, threadRepository.findByChapter_Manuscript_ManuscriptId(manuscript.getManuscriptId()).stream()
                .filter(t -> t.getStatus() == ThreadStatus.RESOLVED).count());
    }

    @Test
    void latestRevisionAndCoverVersionQueries() {
        Chapter chapter = chapterWithRevisions("Розділ", 1, 3);
        flushAndClear();
        assertEquals(3, revisionRepository.findTopByChapter_ChapterIdOrderByVersionNumberDesc(chapter.getChapterId()).orElseThrow().getVersionNumber());
        assertEquals(3, revisionRepository.findAllByChapter_ChapterIdOrderByVersionNumberAsc(chapter.getChapterId()).size());

        assertFalse(coverVersionRepository.existsByManuscript_ManuscriptId(manuscript.getManuscriptId()));
        coverVersionRepository.save(new CoverVersion(manuscript, "c1", UUID.randomUUID(), 1, Instant.now()));
        coverVersionRepository.save(new CoverVersion(manuscript, "c2", UUID.randomUUID(), 2, Instant.now()));
        flushAndClear();

        assertTrue(coverVersionRepository.existsByManuscript_ManuscriptId(manuscript.getManuscriptId()));
        assertEquals(2, coverVersionRepository.findTopByManuscript_ManuscriptIdOrderByVersionNumberDesc(manuscript.getManuscriptId()).orElseThrow().getVersionNumber());
        assertEquals(2, coverVersionRepository.findByManuscript_ManuscriptIdOrderByVersionNumberAsc(manuscript.getManuscriptId()).size());
    }

    @Test
    void deleteManuscript_removesChaptersAndCovers() {
        chapterWithRevisions("Розділ", 1, 2);
        coverVersionRepository.save(new CoverVersion(manuscript, "c1", UUID.randomUUID(), 1, Instant.now()));
        flushAndClear();

        em.createNativeQuery("DELETE FROM manuscripts WHERE manuscript_id = ?1")
                .setParameter(1, manuscript.getManuscriptId()).executeUpdate();
        flushAndClear();

        assertEquals(0, chapterRepository.count());
        assertEquals(0, revisionRepository.count());
        assertEquals(0, coverVersionRepository.count());
    }

    private FeedbackThread newThread() {
        FeedbackThread thread = new FeedbackThread();
        thread.setCreatedByUserId(UUID.randomUUID());
        thread.setStatus(ThreadStatus.OPEN);
        thread.setCreatedAt(Instant.now());
        return thread;
    }
}
