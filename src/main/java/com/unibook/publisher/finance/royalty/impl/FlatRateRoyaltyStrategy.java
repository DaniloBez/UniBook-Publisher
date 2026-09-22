package com.unibook.publisher.finance.royalty.impl;

import com.unibook.publisher.common.enums.RoyaltyStrategyType;
import com.unibook.publisher.finance.entity.Contract;
import com.unibook.publisher.finance.royalty.RoyaltyStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FlatRateRoyaltyStrategy implements RoyaltyStrategy {

    @Override
    public RoyaltyStrategyType getType() {
        return RoyaltyStrategyType.FLAT_RATE;
    }

    @Override
    public BigDecimal calculateRoyalty(Contract contract, BigDecimal salesAmount) {
        return salesAmount
                .multiply(contract.royaltyPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
