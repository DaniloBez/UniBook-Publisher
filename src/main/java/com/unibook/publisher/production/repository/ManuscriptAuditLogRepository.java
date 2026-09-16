package com.unibook.publisher.production.repository;

import com.unibook.publisher.production.entity.ManuscriptAuditLog;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ManuscriptAuditLogRepository {
    private final ConcurrentHashMap<UUID, ManuscriptAuditLog> logs = new ConcurrentHashMap<>();

    public ManuscriptAuditLog save(ManuscriptAuditLog log) {
        logs.put(log.id(), log);
        return log;
    }

    public List<ManuscriptAuditLog> findByManuscriptId(UUID manuscriptId) {
        return logs.values().stream()
                .filter(log -> log.manuscriptId().equals(manuscriptId))
                .sorted(Comparator.comparing(ManuscriptAuditLog::timestamp))
                .toList();
    }
}
