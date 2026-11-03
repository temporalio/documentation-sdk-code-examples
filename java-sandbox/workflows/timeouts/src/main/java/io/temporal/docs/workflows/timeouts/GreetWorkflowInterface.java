package io.temporal.docs.workflows.timeouts;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface GreetWorkflowInterface {
    @WorkflowMethod
    String greet(String name);
}
