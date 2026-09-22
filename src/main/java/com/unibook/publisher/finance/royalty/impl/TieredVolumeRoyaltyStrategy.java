package com.unibook.publisher.finance.royalty.impl;

import com.unibook.publisher.common.enums.RoyaltyStrategyType;
import com.unibook.publisher.finance.entity.Contract;
import com.unibook.publisher.finance.royalty.RoyaltyStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TieredVolumeRoyaltyStrategy implements RoyaltyStrategy {

    private static final BigDecimal FIRST_TIER_LIMIT = new BigDecimal("1000");
    private static final BigDecimal SECOND_TIER_LIMIT = new BigDecimal("5000");

    private static final BigDecimal FIRST_TIER_RATE = new BigDecimal("5");
    private static final BigDecimal SECOND_TIER_RATE = new BigDecimal("7");
    private static final BigDecimal THIRD_TIER_RATE = new BigDecimal("10");

    @Override
    public RoyaltyStrategyType getType() {
        return RoyaltyStrategyType.TIERED_VOLUME;
    }

    @Override
    public BigDecimal calculateRoyalty(Contract contract, BigDecimal salesAmount) {
        BigDecimal remaining = salesAmount;
        BigDecimal royalty = BigDecimal.ZERO;

        BigDecimal firstTierAmount = remaining.min(FIRST_TIER_LIMIT);
        royalty = royalty.add(calculateTier(firstTierAmount, FIRST_TIER_RATE));
        remaining = remaining.subtract(firstTierAmount);

        if (remaining.signum() > 0) {
            BigDecimal secondTierAmount = remaining.min(
                    SECOND_TIER_LIMIT.subtract(FIRST_TIER_LIMIT)
            );

            royalty = royalty.add(calculateTier(secondTierAmount, SECOND_TIER_RATE));
            remaining = remaining.subtract(secondTierAmount);
        }

        if (remaining.signum() > 0) {
            royalty = royalty.add(calculateTier(remaining, THIRD_TIER_RATE));
        }

        return royalty.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTier(BigDecimal amount, BigDecimal rate) {
        return amount
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
