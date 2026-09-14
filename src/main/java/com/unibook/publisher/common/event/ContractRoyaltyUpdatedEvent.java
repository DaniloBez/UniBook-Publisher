package com.unibook.publisher.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ContractRoyaltyUpdatedEvent(UUID contractId, UUID manuscriptId, UUID authorId, BigDecimal newRoyaltyPercent) {
}
