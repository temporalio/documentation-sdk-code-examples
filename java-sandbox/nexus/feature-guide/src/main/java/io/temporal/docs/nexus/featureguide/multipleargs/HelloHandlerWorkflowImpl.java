package io.temporal.docs.nexus.featureguide.multipleargs;

import io.temporal.docs.nexus.featureguide.service.SampleNexusService;

public class HelloHandlerWorkflowImpl implements HelloHandlerWorkflow {
  @Override
  public SampleNexusService.HelloOutput hello(String name, SampleNexusService.Language language) {
    return new SampleNexusService.HelloOutput(language + " hello " + name);
  }
}
