package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GenreRepository extends JpaRepository<Genre, UUID> {
    Optional<Genre> findByGenreName(String name);
    boolean existsByGenreName(String name);
}
