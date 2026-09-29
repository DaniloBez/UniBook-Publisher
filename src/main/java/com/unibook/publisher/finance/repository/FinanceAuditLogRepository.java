package com.unibook.publisher.finance.repository;

import com.unibook.publisher.finance.entity.FinanceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public interface FinanceAuditLogRepository extends JpaRepository<FinanceAuditLog, UUID> {}
