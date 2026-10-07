package com.unibook.publisher.finance.entity.request;

import com.unibook.publisher.common.enums.RoyaltyStrategyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request to simulate the author payout for a given sales amount")
public record PayoutSimulationRequest(
        @Schema(description = "Total sales amount used in the calculation", example = "25000.00", minimum = "0")
        @NotNull
        @DecimalMin(value = "0.0", message = "Сума продажів не може бути відʼємною")
        BigDecimal salesAmount,

        @Schema(description = "Royalty calculation strategy to apply")
        @NotNull
        RoyaltyStrategyType royaltyStrategyType
) {
}
