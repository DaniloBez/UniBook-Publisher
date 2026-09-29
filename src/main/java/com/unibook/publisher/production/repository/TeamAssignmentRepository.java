package com.unibook.publisher.production.repository;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.production.entity.TeamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamAssignmentRepository extends JpaRepository<TeamAssignment, UUID> {
    boolean existsByManuscript_ManuscriptIdAndUserIdAndRole(UUID manuscriptId, UUID userId, UserRole role);
    Optional<TeamAssignment> findByManuscript_ManuscriptIdAndRole(UUID manuscriptId, UserRole role);
}
