package io.temporal.docs.opentelemetryv2;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface GreetingWorkflow {
  @WorkflowMethod
  String greet(String name);
}
