package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.Revision;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RevisionRepository {
    private final ConcurrentHashMap<UUID, Revision> revisions = new ConcurrentHashMap<>();

    public Revision save(Revision revision) {
        revisions.put(revision.revisionId(), revision);
        return revision;
    }

    public Optional<Revision> findById(UUID id) {
        return Optional.ofNullable(revisions.get(id));
    }

    public List<Revision> findAll() {
        return List.copyOf(revisions.values());
    }

    public Optional<Revision> findLatestVersionNumberByChapterId(UUID chapterId) {
        return revisions.values().stream()
                .filter(revision -> revision.chapterId().equals(chapterId))
                .max(Comparator.comparingInt(Revision::versionNumber));
    }

    public List<Revision> findAllByChapterId(UUID chapterId) {
        return revisions.values().stream()
                .filter(revision -> revision.chapterId().equals(chapterId))
                .sorted(Comparator.comparingInt(Revision::versionNumber))
                .toList();
    }
}
