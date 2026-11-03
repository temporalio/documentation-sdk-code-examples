package io.temporal.docs.workflows.workflowstreams.closing;

import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import io.temporal.workflowstreams.WorkflowTopicHandle;
import java.time.Duration;

public class FixedSleepWorkflowImpl implements ChatWorkflow {
  private final WorkflowTopicHandle status;

  @WorkflowInit
  public FixedSleepWorkflowImpl(ChatInput input) {
    status = WorkflowStream.newInstance(input.streamState).topic("status");
  }

  @Override
  public void subscriberAcknowledgedTerminator() {}

  @Override
  public String complete(ChatInput input) {
    String result = "done: " + input.prompt;
    // @@@SNIPSTART java-workflow-streams-fixed-sleep
    // at the end of the workflow method
    status.publish(new StatusEvent("completed", 100, ""));
    Workflow.sleep(Duration.ofSeconds(30));
    return result;
    // @@@SNIPEND
  }
}
