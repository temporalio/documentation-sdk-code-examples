package io.temporal.docs.platform.observability.searchattributes;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class PizzaWorkflowImpl implements PizzaWorkflow {

    private final PizzaActivities activities =
        Workflow.newActivityStub(
            PizzaActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

    @Override
    public int orderPizza(String address) {
        // @@@SNIPSTART java-observability-upsert-search-attributes
        int distance;
        try {
            distance = activities.getDistance(address);
            Workflow.upsertTypedSearchAttributes(Constants.IS_ORDER_FAILED.valueSet(false));
        } catch (ActivityFailure e) {
            Workflow.upsertTypedSearchAttributes(Constants.IS_ORDER_FAILED.valueSet(true));
            throw e;
        }
        // @@@SNIPEND

        // The order succeeded, so the failure flag is no longer needed.
        // @@@SNIPSTART java-observability-remove-search-attribute
        Workflow.upsertTypedSearchAttributes(Constants.IS_ORDER_FAILED.valueUnset());
        // @@@SNIPEND
        return distance;
    }
}
