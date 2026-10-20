package com.unibook.publisher.production.repository;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.production.entity.TeamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeamAssignmentRepository extends JpaRepository<TeamAssignment, UUID> {
    boolean existsByManuscript_ManuscriptIdAndUserIdAndRole(UUID manuscriptId, UUID userId, UserRole role);
    Optional<TeamAssignment> findByManuscript_ManuscriptIdAndRole(UUID manuscriptId, UserRole role);

    @Query("select ta.manuscript.manuscriptId from TeamAssignment ta where ta.userId = :userId and ta.role = :role")
    List<UUID> findManuscriptIdsByUserIdAndRole(@Param("userId") UUID userId, @Param("role") UserRole role);
}
