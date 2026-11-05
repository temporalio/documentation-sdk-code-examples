package io.temporal.docs.bestpractices.dataconversion;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PricingWorkflowImpl implements PricingWorkflow {
    @Override
    public Money addTax(Money price, String region) {
        BigDecimal rate = "EU".equals(region) ? new BigDecimal("0.20") : new BigDecimal("0.10");
        BigDecimal total = price.amount().add(price.amount().multiply(rate))
            .setScale(2, RoundingMode.HALF_UP);
        return new Money(total, price.currency());
    }
}
