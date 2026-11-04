package io.temporal.docs.nexus.standaloneoperations.handler;

import io.temporal.docs.nexus.standaloneoperations.service.GreetingNexusService;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

// The workflow backing the startGreeting Nexus operation. It blocks indefinitely and never
// completes on its own, which keeps the backing standalone Nexus operation in a running state so
// the sample can demonstrate cancel/terminate against it.
@WorkflowInterface
public interface GreetingWorkflow {
  @WorkflowMethod
  GreetingNexusService.GreetingOutput greet(GreetingNexusService.GreetingInput input);
}
