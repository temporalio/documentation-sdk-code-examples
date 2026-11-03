package io.temporal.docs.workflows.workflowstreams.closing;

import io.temporal.workflowstreams.WorkflowStreamState;

public class ChatInput {
  public String prompt;
  public WorkflowStreamState streamState;

  public ChatInput() {}

  public ChatInput(String prompt) {
    this.prompt = prompt;
  }
}
