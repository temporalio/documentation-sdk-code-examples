package io.temporal.docs.nexus.featureguide.handler;

import io.temporal.docs.nexus.featureguide.service.SampleNexusService;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloHandlerWorkflow {
  @WorkflowMethod
  SampleNexusService.HelloOutput hello(SampleNexusService.HelloInput input);
}
