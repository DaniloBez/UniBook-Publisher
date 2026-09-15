package com.unibook.publisher.finance.repository;

import com.unibook.publisher.finance.entity.FinanceAuditLog;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FinanceAuditLogRepository {
    private final Map<UUID, FinanceAuditLog> logs = new ConcurrentHashMap<>();

    public FinanceAuditLog save(FinanceAuditLog log) {
        UUID id = log.id() != null ? log.id() : UUID.randomUUID();
        FinanceAuditLog toSave = new FinanceAuditLog(
                id,
                log.contractId(),
                log.changedByUserId(),
                log.oldRoyaltyPercent(),
                log.newRoyaltyPercent(),
                log.changeReason(),
                log.timestamp() != null ? log.timestamp() : Instant.now()
        );
        logs.put(id, toSave);
        return toSave;
    }

    public List<FinanceAuditLog> findByContractId(UUID contractId) {
        return logs.values().stream()
                .filter(l -> l.contractId().equals(contractId))
                .sorted(Comparator.comparing(FinanceAuditLog::timestamp).reversed())
                .toList();
    }
}
