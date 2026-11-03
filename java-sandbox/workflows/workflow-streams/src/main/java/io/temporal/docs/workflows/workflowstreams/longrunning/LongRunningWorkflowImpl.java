package io.temporal.docs.workflows.workflowstreams.longrunning;

// @@@SNIPSTART java-workflow-streams-continue-as-new
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import java.time.Duration;

public class LongRunningWorkflowImpl implements LongRunningWorkflow {
  private final WorkflowStream stream;
  private int itemsProcessed;

  @WorkflowInit
  public LongRunningWorkflowImpl(WorkflowInput input) {
    stream = WorkflowStream.newInstance(input.streamState);
    itemsProcessed = input.itemsProcessed;
  }

  @Override
  public void execute(WorkflowInput input) {
    while (true) {
      doOneIteration();
      itemsProcessed++;

      if (Workflow.getInfo().isContinueAsNewSuggested()) {
        stream.continueAsNew(
            state -> {
              WorkflowInput next = new WorkflowInput();
              next.itemsProcessed = itemsProcessed; // your own state, carried across
              next.streamState = state;             // the captured stream state
              return new Object[] {next};
            });
      }
    }
  }

  private void doOneIteration() {
    stream.topic("items").publish(itemsProcessed);
    Workflow.sleep(Duration.ofSeconds(1));
  }
}
// @@@SNIPEND
