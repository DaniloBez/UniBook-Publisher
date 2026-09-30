package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
    @Query("SELECT DISTINCT c FROM Chapter c LEFT JOIN FETCH c.revisions WHERE c.manuscript.manuscriptId = :manuscriptId ORDER BY c.chapterIndex")
    List<Chapter> findByManuscriptIdWithRevisions(@Param("manuscriptId") UUID manuscriptId);

    List<Chapter> findByManuscript_ManuscriptIdOrderByChapterIndex(UUID manuscriptId);
}
