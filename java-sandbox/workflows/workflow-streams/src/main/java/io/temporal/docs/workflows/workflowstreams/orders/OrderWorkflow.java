package io.temporal.docs.workflows.workflowstreams.orders;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderWorkflow {
  @WorkflowMethod
  void execute(OrderInput input);
}
