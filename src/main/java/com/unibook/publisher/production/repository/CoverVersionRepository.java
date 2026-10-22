package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.CoverVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoverVersionRepository extends JpaRepository<CoverVersion, UUID> {
    List<CoverVersion> findByManuscript_ManuscriptIdOrderByVersionNumberAsc(UUID manuscriptId);

    Optional<CoverVersion> findTopByManuscript_ManuscriptIdOrderByVersionNumberDesc(UUID manuscriptId);

    boolean existsByManuscript_ManuscriptIdAndApprovedTrue(UUID manuscriptId);

    @Modifying
    @Query("UPDATE CoverVersion c SET c.isApproved = false WHERE c.manuscript.manuscriptId = :manuscriptId AND c.isApproved = true")
    void unapproveAllByManuscript_ManuscriptId(@Param("manuscriptId") UUID manuscriptId);

    CoverVersion findByIdAndManuscriptId(UUID id, UUID manuscriptId);
}
