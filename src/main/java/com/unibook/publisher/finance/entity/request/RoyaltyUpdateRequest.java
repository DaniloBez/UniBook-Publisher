package com.unibook.publisher.finance.entity.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RoyaltyUpdateRequest(
        @NotNull
        @DecimalMin(value = "1.0", message = "Ставка роялті не може бути меншою за 1.0%")
        @DecimalMax(value = "50.0", message = "Ставка роялті не може перевищувати 50.0%")
        BigDecimal newRoyaltyPercent,

        @NotNull
        @DecimalMin(value = "0.0", message = "Розмір авансу не може бути відʼємним")
        BigDecimal newAdvance,

        @NotBlank
        @Size(max = 1000)
        String reason
) {
}
