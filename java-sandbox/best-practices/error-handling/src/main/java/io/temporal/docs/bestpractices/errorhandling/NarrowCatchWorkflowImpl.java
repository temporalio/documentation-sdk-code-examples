package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class NarrowCatchWorkflowImpl implements OrderWorkflow {
    private final PaymentActivities activities = Workflow.newActivityStub(
        PaymentActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
            .build());

    @Override
    public Result processOrder(Order order) {
        // @@@SNIPSTART java-error-handling-narrow-catch
        try {
            return doWorkflowLogic(order);
        } catch (ActivityFailure e) {
            if (e.getCause() instanceof ApplicationFailure appFailure
                && "ValidationError".equals(appFailure.getType())) {
                return defaultResult();
            }
            throw e; // don't recognize it — propagate
        }
        // @@@SNIPEND
    }

    private Result doWorkflowLogic(Order order) {
        return Result.approved(activities.processCreditCard(order.getOrderId()));
    }

    private Result defaultResult() {
        return Result.rejected("default");
    }
}
