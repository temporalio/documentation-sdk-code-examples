package io.temporal.docs.workflows.workflowstreams.llm;

// @@@SNIPSTART java-workflow-streams-llm-workflow
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import java.time.Duration;

public class ChatWorkflowImpl implements ChatWorkflow {
  private final LlmActivities activities =
      Workflow.newActivityStub(
          LlmActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(5)).build());

  private boolean subscriberDone = false;

  @WorkflowInit
  public ChatWorkflowImpl(ChatInput input) {
    WorkflowStream.newInstance(input.streamState);
  }

  @Override
  public void subscriberAcknowledgedTerminator() {
    subscriberDone = true;
  }

  @Override
  public String complete(ChatInput input) {
    String result = activities.streamCompletion(input.prompt);

    // Wait for the subscriber to ack the terminal `close` event. The timeout
    // is a fallback for when no subscriber is attached; with the ack, the
    // typical case exits as soon as the subscriber confirms.
    Workflow.await(Duration.ofSeconds(30), () -> subscriberDone);
    return result;
  }
}
// @@@SNIPEND
