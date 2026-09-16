package com.unibook.publisher.finance.entity.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayoutSimulationRequest(
        @NotNull
        @DecimalMin(value = "0.0", message = "Сума продажів не може бути відʼємною")
        BigDecimal salesAmount
) {
}
