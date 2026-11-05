package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class CauseChainWorkflowImpl implements OrderWorkflow {
    private final PaymentActivities activities = Workflow.newActivityStub(
        PaymentActivities.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public Result processOrder(Order order) {
        String orderId = order.getOrderId();
        // @@@SNIPSTART java-error-handling-read-cause-chain
        try {
            return Result.approved(activities.processCreditCard(orderId));
        } catch (ActivityFailure e) {
            if (e.getCause() instanceof ApplicationFailure appFailure) {
                if ("ValidationError".equals(appFailure.getType())) {
                    return Result.rejected(appFailure.getOriginalMessage());
                }
            }
            throw e;
        }
        // @@@SNIPEND
    }
}
