package com.unibook.publisher.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "finance_audit_logs")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FinanceAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "changed_by_user_id", nullable = false)
    private UUID changedByUserId;

    @Column(name = "old_royalty_percent", nullable = false)
    private BigDecimal oldRoyaltyPercent;

    @Column(name = "new_royalty_percent", nullable = false)
    private BigDecimal newRoyaltyPercent;

    @Column(name = "old_advance_payment", nullable = false)
    private BigDecimal oldAdvancePayment;

    @Column(name = "new_advance_payment", nullable = false)
    private BigDecimal newAdvancePayment;

    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
