package com.unibook.publisher.finance.royalty;

import com.unibook.publisher.common.enums.RoyaltyStrategyType;
import com.unibook.publisher.finance.entity.Contract;

import java.math.BigDecimal;

public interface RoyaltyStrategy {

    RoyaltyStrategyType getType();

    BigDecimal calculateRoyalty(Contract contract, BigDecimal salesAmount);
}
