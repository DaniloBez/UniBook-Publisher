package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.Revision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RevisionRepository extends JpaRepository<Revision, UUID> {
    Optional<Revision> findTopByChapter_ChapterIdOrderByVersionNumberDesc(UUID chapterId);

    List<Revision> findAllByChapter_ChapterIdOrderByVersionNumberAsc(UUID chapterId);
}
