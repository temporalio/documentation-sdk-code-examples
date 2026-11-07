package io.temporal.docs.bestpractices.dataconversion;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PricingWorkflow {
    @WorkflowMethod
    Money addTax(Money price, String region);
}
