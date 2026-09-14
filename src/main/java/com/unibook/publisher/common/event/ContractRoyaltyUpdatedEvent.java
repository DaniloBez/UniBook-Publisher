package com.unibook.publisher.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ContractRoyaltyUpdatedEvent(
        UUID contractId,
        UUID manuscriptId,
        String manuscriptTitle,
        UUID authorId,
        BigDecimal newRoyaltyPercent
) {
}
