package io.temporal.docs.platform.observability.searchattributes;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PizzaWorkflow {
    @WorkflowMethod
    int orderPizza(String address);
}
