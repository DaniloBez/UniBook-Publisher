package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.Genre;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class GenreRepository {
    private final ConcurrentHashMap<UUID, Genre> genres = new ConcurrentHashMap<>();

    public Genre save(Genre genre) {
        genres.put(genre.genreId(), genre);
        return genre;
    }

    public Optional<Genre> findById(UUID genreId) {
        return Optional.ofNullable(genres.get(genreId));
    }

    public List<Genre> findAll() {
        return List.copyOf(genres.values());
    }
}