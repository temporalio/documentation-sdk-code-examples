package io.temporal.docs.nexus.featureguide.caller;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface EchoCallerWorkflow {
  @WorkflowMethod
  String echo(String message);
}
