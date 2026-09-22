package com.unibook.publisher.common.exception.business;

import com.unibook.publisher.common.enums.RoyaltyStrategyType;

public class UnsupportedRoyaltyStrategyException extends BusinessRuleViolationException {
    public UnsupportedRoyaltyStrategyException(RoyaltyStrategyType type) {
        super("Непідтримуваний тип стратегії виплати роялті: " + type);
    }
}
