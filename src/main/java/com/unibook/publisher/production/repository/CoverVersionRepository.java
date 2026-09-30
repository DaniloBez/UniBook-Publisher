package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.CoverVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoverVersionRepository extends JpaRepository<CoverVersion, UUID> {
    List<CoverVersion> findByManuscript_ManuscriptIdOrderByVersionNumberAsc(UUID manuscriptId);

    Optional<CoverVersion> findTopByManuscript_ManuscriptIdOrderByVersionNumberDesc(UUID manuscriptId);

    boolean existsByManuscript_ManuscriptId(UUID manuscriptId);
}
