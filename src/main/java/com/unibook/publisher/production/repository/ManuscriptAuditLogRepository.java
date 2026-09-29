package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.ManuscriptAuditLog;
import com.unibook.publisher.production.entity.response.ManuscriptAuditLogResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public interface ManuscriptAuditLogRepository extends JpaRepository<ManuscriptAuditLog, UUID> {
    @Query("SELECT l FROM ManuscriptAuditLog l WHERE l.manuscript.manuscriptId = :manuscriptId ORDER BY l.timestamp DESC")
    List<ManuscriptAuditLog> findByManuscriptId(@Param("manuscriptId") UUID manuscriptId);
}
