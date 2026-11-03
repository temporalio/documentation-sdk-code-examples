package io.temporal.docs.workflows.workflowstreams.longrunning;

import io.temporal.workflow.ContinueAsNewOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import io.temporal.workflowstreams.WorkflowStreamState;
import java.time.Duration;

public class ExplicitContinueAsNewWorkflowImpl implements LongRunningWorkflow {
  private final WorkflowStream stream;
  private int itemsProcessed;

  @WorkflowInit
  public ExplicitContinueAsNewWorkflowImpl(WorkflowInput input) {
    stream = WorkflowStream.newInstance(input.streamState);
    itemsProcessed = input.itemsProcessed;
  }

  @Override
  public void execute(WorkflowInput input) {
    stream.topic("items").publish(itemsProcessed);
    itemsProcessed++;
    if (itemsProcessed > 2) {
      // Give in-flight polls time to fetch the last item before the Workflow returns.
      Workflow.sleep(Duration.ofSeconds(2));
      return;
    }
    // @@@SNIPSTART java-workflow-streams-continue-as-new-explicit
    stream.detachPollers();
    Workflow.await(() -> Workflow.isEveryHandlerFinished());
    WorkflowStreamState state = stream.getState(Duration.ofMinutes(30)); // custom publisher TTL
    WorkflowInput next = new WorkflowInput();
    next.itemsProcessed = itemsProcessed;
    next.streamState = state;
    Workflow.continueAsNew(
        ContinueAsNewOptions.newBuilder().setTaskQueue("other-tq").build(), next);
    // @@@SNIPEND
  }
}
