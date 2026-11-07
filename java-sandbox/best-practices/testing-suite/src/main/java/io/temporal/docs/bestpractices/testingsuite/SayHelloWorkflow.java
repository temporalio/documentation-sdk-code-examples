package io.temporal.docs.bestpractices.testingsuite;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SayHelloWorkflow {
  @WorkflowMethod
  String sayHello(String name);
}
