package io.temporal.docs.nexus.featureguide.messaging;

import io.temporal.docs.nexus.featureguide.messaging.GreetingNexusService.ApproveInput;
import io.temporal.workflow.Workflow;

/** Waits for an approval Signal, then completes. */
public class GreetingWorkflowImpl implements GreetingWorkflow {
  private String approvedBy;

  @Override
  public String run() {
    Workflow.await(() -> approvedBy != null);
    return "Approved by " + approvedBy;
  }

  @Override
  public void approve(ApproveInput input) {
    approvedBy = input.getName();
  }
}
