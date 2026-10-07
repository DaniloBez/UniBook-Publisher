package com.unibook.publisher.finance.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request to change royalty terms of a draft contract with a recorded reason")
public record RoyaltyUpdateRequest(
        @Schema(description = "New royalty percent of sales", example = "15.0", minimum = "1", maximum = "50")
        @NotNull
        @DecimalMin(value = "1.0", message = "Ставка роялті не може бути меншою за 1.0%")
        @DecimalMax(value = "50.0", message = "Ставка роялті не може перевищувати 50.0%")
        BigDecimal newRoyaltyPercent,

        @Schema(description = "New advance payment amount", example = "2000.00", minimum = "0")
        @NotNull
        @DecimalMin(value = "0.0", message = "Розмір авансу не може бути відʼємним")
        BigDecimal newAdvance,

        @Schema(description = "Reason for the change, stored in the finance audit log", example = "Terms renegotiated with the author", maxLength = 1000)
        @NotBlank
        @Size(max = 1000)
        String reason
) {
}
