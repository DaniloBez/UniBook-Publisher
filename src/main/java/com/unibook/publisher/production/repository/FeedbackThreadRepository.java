package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.FeedbackThread;
import com.unibook.publisher.production.enums.ThreadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackThreadRepository extends JpaRepository<FeedbackThread, UUID> {
    @Query("SELECT DISTINCT t FROM FeedbackThread t LEFT JOIN FETCH t.messages WHERE t.chapter.chapterId = :chapterId ORDER BY t.createdAt")
    List<FeedbackThread> findByChapterIdWithMessages(@Param("chapterId") UUID chapterId);

    @Query("SELECT DISTINCT t FROM FeedbackThread t LEFT JOIN FETCH t.messages WHERE t.chapter.chapterId = :chapterId AND t.status = :status ORDER BY t.createdAt")
    List<FeedbackThread> findByChapterIdAndStatusWithMessages(@Param("chapterId") UUID chapterId, @Param("status") ThreadStatus status);

    List<FeedbackThread> findByChapter_Manuscript_ManuscriptId(UUID manuscriptId);
}
