package com.unibook.publisher.finance.entity;

import com.unibook.publisher.common.enums.ContractStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


public record Contract(
        UUID id,
        UUID manuscriptId,
        String manuscriptTitle,
        UUID authorId,
        BigDecimal royaltyPercent,
        BigDecimal advancePayment,
        ContractStatus status,
        Instant authorConfirmedAt,
        Instant createdAt
) {
    public Contract withUpdatedRoyalty(BigDecimal newRoyaltyPercent, BigDecimal newAdvance) {
        return new Contract(
                id, manuscriptId, manuscriptTitle, authorId,
                newRoyaltyPercent, newAdvance, status,
                null, //the agreement resets with any royalty change
                createdAt
        );
    }

    public Contract confirmedByAuthor(Instant confirmedAt) {
        return new Contract(id, manuscriptId, manuscriptTitle, authorId, royaltyPercent, advancePayment, status, confirmedAt, createdAt);
    }

    public Contract activated() {
        return new Contract(id, manuscriptId, manuscriptTitle, authorId, royaltyPercent, advancePayment, ContractStatus.ACTIVE, authorConfirmedAt, createdAt);
    }
}
