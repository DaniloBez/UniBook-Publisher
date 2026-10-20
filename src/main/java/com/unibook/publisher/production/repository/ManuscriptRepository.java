package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.Manuscript;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManuscriptRepository extends JpaRepository<Manuscript, UUID> {
    @Query("SELECT DISTINCT m FROM Manuscript m LEFT JOIN FETCH m.genres WHERE m.status = :status")
    List<Manuscript> findByStatusWithGenres(ManuscriptStatus status);

    @Query("SELECT DISTINCT m FROM Manuscript m LEFT JOIN FETCH m.genres WHERE m.manuscriptId = :id")
    Optional<Manuscript> findByIdWithGenres(@Param("id") UUID id);

    @Query("SELECT DISTINCT m FROM Manuscript m LEFT JOIN FETCH m.genres")
    List<Manuscript> findAllWithGenres();

    @Query("select distinct m from Manuscript m left join fetch m.genres where m.authorId = :authorId")
    List<Manuscript> findByAuthorIdWithGenres(@Param("authorId") UUID authorId);

    @Query("select distinct m from Manuscript m left join fetch m.genres where m.manuscriptId in :ids")
    List<Manuscript> findAllByIdWithGenres(@Param("ids") Collection<UUID> ids);
}
