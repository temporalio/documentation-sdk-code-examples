package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

// Anti-pattern kept here so its effect can be tested: don't copy this class.
public class SwallowingWorkflowImpl implements OrderWorkflow {
    private static final Logger logger = Workflow.getLogger(SwallowingWorkflowImpl.class);

    private final PaymentActivities activities = Workflow.newActivityStub(
        PaymentActivities.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public Result processOrder(Order order) {
        // @@@SNIPSTART java-error-handling-swallow-anti-pattern
        // Anti-pattern: never do this in Workflow, Update, or Signal code.
        try {
            return doWorkflowLogic(order);
        } catch (Throwable t) {
            logger.error("workflow failed", t);
            return defaultResult();
        }
        // @@@SNIPEND
    }

    private Result doWorkflowLogic(Order order) {
        Workflow.sleep(Duration.ofHours(1));
        return Result.approved(activities.processCreditCard(order.getOrderId()));
    }

    private Result defaultResult() {
        return Result.rejected("default");
    }
}
