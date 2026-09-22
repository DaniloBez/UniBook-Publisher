package com.unibook.publisher.finance.royalty.impl;

import com.unibook.publisher.common.enums.RoyaltyStrategyType;
import com.unibook.publisher.finance.entity.Contract;
import com.unibook.publisher.finance.royalty.RoyaltyStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AdvanceRecoupmentRoyaltyStrategy implements RoyaltyStrategy {

    @Override
    public RoyaltyStrategyType getType() {
        return RoyaltyStrategyType.ADVANCE_RECOUPMENT;
    }

    @Override
    public BigDecimal calculateRoyalty(Contract contract, BigDecimal salesAmount) {
        BigDecimal royalty = salesAmount
                .multiply(contract.royaltyPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return royalty
                .subtract(contract.advancePayment()) //subtracting like in case when advance is an amount already paid and recouped against future royalties
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
