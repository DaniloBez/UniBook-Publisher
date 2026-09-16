package com.unibook.publisher.finance.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record PayoutSimulationResponse(
        @JsonProperty("contract_id")
        UUID contractId,

        @JsonProperty("sales_amount")
        BigDecimal salesAmount,

        @JsonProperty("royalty_percent")
        BigDecimal royaltyPercent,

        @JsonProperty("advance_payment")
        BigDecimal advancePayment,

        @JsonProperty("calculated_royalty")
        BigDecimal calculatedRoyalty,

        @JsonProperty("total_payout")
        BigDecimal totalPayout
) {
}
