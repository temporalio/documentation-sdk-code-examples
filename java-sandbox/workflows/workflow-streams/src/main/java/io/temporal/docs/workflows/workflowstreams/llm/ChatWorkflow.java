package io.temporal.docs.workflows.workflowstreams.llm;

// @@@SNIPSTART java-workflow-streams-llm-workflow-interface
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ChatWorkflow {
  @WorkflowMethod
  String complete(ChatInput input);

  @SignalMethod
  void subscriberAcknowledgedTerminator();
}
// @@@SNIPEND
