package com.unibook.publisher.finance.entity.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ContractCreateRequest(
        @NotNull
        UUID manuscriptId,

        @NotNull
        UUID authorId,

        @NotNull
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        BigDecimal royaltyPercent,

        @NotNull
        @DecimalMin(value = "0.0")
        BigDecimal advancePayment
) {}