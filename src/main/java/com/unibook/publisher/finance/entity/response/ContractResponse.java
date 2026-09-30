package com.unibook.publisher.finance.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.publisher.finance.entity.Contract;
import com.unibook.publisher.common.enums.ContractStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContractResponse(
        @JsonProperty("contract_id")
        UUID contractId,

        @JsonProperty("manuscript_id")
        UUID manuscriptId,

        String title,

        @JsonProperty("author_id")
        UUID authorId,

        @JsonProperty("royalty_percent")
        BigDecimal royaltyPercent,

        @JsonProperty("advance_payment")
        BigDecimal advancePayment,

        ContractStatus status,

        @JsonProperty("author_confirmed_at")
        Instant authorConfirmedAt,

        @JsonProperty("created_at")
        Instant createdAt
) {
    public static ContractResponse from(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getManuscriptId(),
                contract.getTitle(),
                contract.getAuthorId(),
                contract.getRoyaltyPercent(),
                contract.getAdvancePayment(),
                contract.getStatus(),
                contract.getAuthorConfirmedAt(),
                contract.getCreatedAt()
        );
    }
}
