package io.temporal.docs.bestpractices.errorhandling;

import java.math.BigDecimal;

public class Order {
    private String orderId;
    private BigDecimal total;

    public Order() {}

    public Order(String orderId, BigDecimal total) {
        this.orderId = orderId;
        this.total = total;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
