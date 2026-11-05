package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.failure.ApplicationFailure;
import java.math.BigDecimal;

public class OrderValidationWorkflowImpl implements OrderWorkflow {
    @Override
    public Result processOrder(Order order) {
        // @@@SNIPSTART java-error-handling-fail-workflow
        if (order.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApplicationFailure.newNonRetryableFailure(
                "Order total must be positive: " + order.getTotal(), "InvalidOrderTotal");
        }
        // @@@SNIPEND
        return Result.approved("Order " + order.getOrderId() + " is valid");
    }
}
