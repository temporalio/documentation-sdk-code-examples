package io.temporal.docs.nexus.featureguide.caller;

import io.temporal.docs.nexus.featureguide.service.SampleNexusService;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloCallerWorkflow {
  @WorkflowMethod
  String hello(String message, SampleNexusService.Language language);
}
