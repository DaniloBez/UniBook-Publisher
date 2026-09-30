package com.unibook.publisher.finance.repository;

import com.unibook.publisher.finance.entity.FinanceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FinanceAuditLogRepository extends JpaRepository<FinanceAuditLog, UUID> {}
