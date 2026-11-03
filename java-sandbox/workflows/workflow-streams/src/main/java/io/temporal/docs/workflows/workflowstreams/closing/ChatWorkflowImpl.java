package io.temporal.docs.workflows.workflowstreams.closing;

import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import io.temporal.workflowstreams.WorkflowTopicHandle;
import java.time.Duration;

// @@@SNIPSTART java-workflow-streams-ack-handshake
public class ChatWorkflowImpl implements ChatWorkflow {
  private final WorkflowTopicHandle status;
  private boolean subscriberDone = false;

  @WorkflowInit
  public ChatWorkflowImpl(ChatInput input) {
    status = WorkflowStream.newInstance(input.streamState).topic("status");
  }

  @Override // annotated with @SignalMethod on the workflow interface
  public void subscriberAcknowledgedTerminator() {
    subscriberDone = true;
  }

  @Override
  public String complete(ChatInput input) {
    // ... do work and publish events ...
    String result = "done: " + input.prompt;
    status.publish(new StatusEvent("completed", 100, ""));

    // Returns true if the ack arrived, false on timeout. Either way, fall through.
    Workflow.await(Duration.ofSeconds(30), () -> subscriberDone);
    return result;
  }
}
// @@@SNIPEND
