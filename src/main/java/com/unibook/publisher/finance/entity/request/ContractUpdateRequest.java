package com.unibook.publisher.finance.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request to replace the financial terms of a draft contract")
public record ContractUpdateRequest(
        @Schema(description = "Royalty percent of sales paid to the author", example = "12.5", minimum = "0", maximum = "100")
        @NotNull()
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        BigDecimal royaltyPercent,

        @Schema(description = "Advance payment to the author", example = "1500.00", minimum = "0")
        @NotNull
        @DecimalMin(value = "0.0")
        BigDecimal advancePayment
) {}
