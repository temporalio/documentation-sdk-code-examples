package io.temporal.docs.bestpractices.dataconversion;

import java.math.BigDecimal;

// A value type with no Jackson annotations and no default constructor, so the default Jackson
// converter can serialize it but can't deserialize it. MoneyPayloadConverter handles it instead.
public final class Money {
    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    // Formats as "12.34 USD".
    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }

    public static Money parse(String text) {
        String[] parts = text.split(" ");
        return new Money(new BigDecimal(parts[0]), parts[1]);
    }
}
