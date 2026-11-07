package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PaymentActivities {
    String processCreditCard(String orderId);

    String charge(Order order);

    void longRunningWork();

    void compensate();
}
