package io.temporal.docs.workflows.workflowstreams.longrunning;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface LongRunningWorkflow {
  @WorkflowMethod
  void execute(WorkflowInput input);
}
