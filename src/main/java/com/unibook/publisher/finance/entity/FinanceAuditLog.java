package com.unibook.publisher.finance.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FinanceAuditLog(
        UUID id,
        UUID contractId,
        UUID changedByUserId,
        BigDecimal oldRoyaltyPercent,
        BigDecimal newRoyaltyPercent,
        String changeReason,
        Instant timestamp
) {
}
