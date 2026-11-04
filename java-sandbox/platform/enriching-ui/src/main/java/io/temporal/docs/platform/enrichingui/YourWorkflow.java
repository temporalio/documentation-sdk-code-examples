package io.temporal.docs.platform.enrichingui;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface YourWorkflow {
    @WorkflowMethod
    String yourWorkflowMethod(String input);
}
