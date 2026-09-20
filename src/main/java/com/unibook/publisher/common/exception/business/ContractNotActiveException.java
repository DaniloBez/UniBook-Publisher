package com.unibook.publisher.common.exception.business;

import java.util.UUID;

public class ContractNotActiveException extends BusinessRuleViolationException {
    public ContractNotActiveException(UUID contractId) {
        super(String.format("Контракт [ID: %s] не є активним", contractId));
    }
}
