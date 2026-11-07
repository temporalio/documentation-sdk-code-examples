package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderWorkflow {
    @WorkflowMethod
    Result processOrder(Order order);
}
