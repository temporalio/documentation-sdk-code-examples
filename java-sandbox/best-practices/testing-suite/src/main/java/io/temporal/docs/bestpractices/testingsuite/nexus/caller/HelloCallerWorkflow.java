package io.temporal.docs.bestpractices.testingsuite.nexus.caller;

import io.temporal.docs.bestpractices.testingsuite.nexus.service.SampleNexusService;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloCallerWorkflow {
  @WorkflowMethod
  String hello(String message, SampleNexusService.Language language);
}
