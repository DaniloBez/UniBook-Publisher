package com.unibook.publisher.finance.entity;

import com.unibook.publisher.common.enums.ContractStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contract_id")
    private UUID id;

    @Column(name = "manuscript_id", nullable = false, unique = true)
    private UUID manuscriptId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "royalty_percent", nullable = false)
    private BigDecimal royaltyPercent;

    @Column(name = "advance_payment", nullable = false)
    private BigDecimal advancePayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;

    @Column(name = "author_confirmed_at")
    private Instant authorConfirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
