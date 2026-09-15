package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.Chapter;
import com.unibook.publisher.production.entity.Manuscript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ChapterRepository {
    private final ConcurrentHashMap<UUID, Chapter> chapters = new ConcurrentHashMap<>();

    public Chapter save(Chapter chapter) {
        chapters.put(chapter.chapterId(), chapter);
        return chapter;
    }

    public Optional<Chapter> findById(UUID chapterId) {
        return Optional.ofNullable(chapters.get(chapterId));
    }

    public List<Chapter> findByManuscriptId(Manuscript manuscript) {
        return chapters.values().stream()
                .filter(chapter -> chapter.manuscriptId().equals(manuscript.manuscriptId()))
                .toList();
    }

    public List<Chapter> findAll() {
        return List.copyOf(chapters.values());
    }
}
