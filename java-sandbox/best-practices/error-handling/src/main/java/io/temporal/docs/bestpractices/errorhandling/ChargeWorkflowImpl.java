package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ChargeWorkflowImpl implements OrderWorkflow {
    private final PaymentActivities activities = Workflow.newActivityStub(
        PaymentActivities.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public Result processOrder(Order order) {
        // @@@SNIPSTART java-error-handling-handle-activity-failure
        try {
            return Result.approved(activities.charge(order));
        } catch (ActivityFailure e) {
            if (e.getCause() instanceof CanceledFailure) {
                throw e; // never swallow cancellation
            }
            if (e.getCause() instanceof ApplicationFailure appFailure
                && "PaymentDeclined".equals(appFailure.getType())) {
                return Result.declined(appFailure.getOriginalMessage());
            }
            throw e; // don't recognize it — propagate
        }
        // @@@SNIPEND
    }
}
