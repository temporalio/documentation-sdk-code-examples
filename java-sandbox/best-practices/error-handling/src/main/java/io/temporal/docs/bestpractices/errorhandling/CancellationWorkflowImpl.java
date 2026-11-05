package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class CancellationWorkflowImpl implements OrderWorkflow {
    private final PaymentActivities activities = Workflow.newActivityStub(
        PaymentActivities.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofHours(1)).build());

    @Override
    public Result processOrder(Order order) {
        // @@@SNIPSTART java-error-handling-cancellation-cleanup
        try {
            activities.longRunningWork();
        } catch (ActivityFailure e) {
            if (e.getCause() instanceof CanceledFailure) {
                // The canceled scope would cancel this call right away, so run it detached.
                Workflow.newDetachedCancellationScope(() -> activities.compensate()).run();
            }
            throw e; // rethrow so no further Workflow logic runs
        }
        // @@@SNIPEND
        return Result.approved("done");
    }
}
