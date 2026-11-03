package io.temporal.docs.workflows.workflowstreams.enable;

// @@@SNIPSTART java-workflow-streams-enable
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import io.temporal.workflowstreams.WorkflowStreamState;

class OrderInput {
  public String orderId;
  public WorkflowStreamState streamState;
}

public class OrderWorkflowImpl implements OrderWorkflow {
  private final WorkflowStream stream;

  @WorkflowInit
  public OrderWorkflowImpl(OrderInput input) {
    stream = WorkflowStream.newInstance(input.streamState);
  }

  @Override
  public void execute(OrderInput input) {
    // ... rest of the workflow
  }
}
// @@@SNIPEND
