package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ManuscriptRepository {
    private final ConcurrentHashMap<UUID, Manuscript> manuscripts = new ConcurrentHashMap<>();

    public Manuscript save(Manuscript manuscript) {
        UUID id = manuscript.manuscriptId() != null ? manuscript.manuscriptId() : UUID.randomUUID();
        Manuscript toSave = new Manuscript(
                id,
                manuscript.title(),
                manuscript.authorId(),
                manuscript.status(),
                manuscript.genreIds(),
                manuscript.annotation(),
                manuscript.draftFileUrl(),
                manuscript.submittedAt()
        );
        manuscripts.put(id, toSave);
        return toSave;
    }

    public Optional<Manuscript> findById(UUID manuscriptId) {
        return Optional.ofNullable(manuscripts.get(manuscriptId));
    }

    public List<Manuscript> findByAuthorId(UUID authorId) {
        return manuscripts.values().stream()
                .filter(m -> m.authorId().equals(authorId))
                .toList();
    }

    public List<Manuscript> findByGenreId(UUID genreId) {
        return manuscripts.values().stream()
                .filter(m -> m.genreIds().contains(genreId))
                .toList();
    }

    public List<Manuscript> findByStatus(ManuscriptStatus status) {
        return manuscripts.values().stream()
                .filter(m -> m.status() == status)
                .toList();
    }

    public List<Manuscript> findAll() {
        return List.copyOf(manuscripts.values());
    }
}
