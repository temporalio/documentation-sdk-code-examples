package io.temporal.docs.workflows.workflowstreams.orders;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface OrderActivities {
  void validateOrder(String orderId);

  void chargePayment(String orderId);

  void dispatchOrder(String orderId);
}
