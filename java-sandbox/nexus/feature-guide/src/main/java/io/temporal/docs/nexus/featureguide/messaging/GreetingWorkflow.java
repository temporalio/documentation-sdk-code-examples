package io.temporal.docs.nexus.featureguide.messaging;

import io.temporal.docs.nexus.featureguide.messaging.GreetingNexusService.ApproveInput;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface GreetingWorkflow {
  @WorkflowMethod
  String run();

  @SignalMethod
  void approve(ApproveInput input);
}
