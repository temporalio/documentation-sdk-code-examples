package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.failure.ApplicationFailure;

public class PaymentActivitiesImpl implements PaymentActivities {
    @Override
    public String processCreditCard(String orderId) {
        if (orderId.startsWith("bad-card")) {
            throw ApplicationFailure.newNonRetryableFailure(
                "Invalid credit card number", "ValidationError");
        }
        return "Card processed for " + orderId;
    }

    @Override
    public String charge(Order order) {
        if (order.getOrderId().startsWith("declined")) {
            throw new PaymentDeclinedException("Insufficient funds", order.getOrderId());
        }
        return "Charged " + order.getTotal();
    }

    @Override
    public void longRunningWork() {}

    @Override
    public void compensate() {}
}
