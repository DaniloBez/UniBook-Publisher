package com.unibook.publisher.common.event;

import java.util.UUID;

public record ContractConfirmedEvent(
        UUID contractId,
        UUID manuscriptId,
        String manuscriptTitle,
        UUID authorId
) {
}
