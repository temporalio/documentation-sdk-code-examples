package io.temporal.docs.bestpractices.testingsuite;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CancellableGreetingWorkflow {
  @WorkflowMethod
  String sayHello(String name);

  @SignalMethod
  void cancelActivity();
}
