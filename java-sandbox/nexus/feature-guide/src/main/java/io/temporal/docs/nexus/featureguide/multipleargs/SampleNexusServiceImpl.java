package io.temporal.docs.nexus.featureguide.multipleargs;

import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.docs.nexus.featureguide.service.SampleNexusService;
import io.temporal.nexus.TemporalNexusClient;
import io.temporal.nexus.TemporalOperation;
import io.temporal.nexus.TemporalOperationResult;
import io.temporal.nexus.TemporalOperationStartContext;

/** Maps the single Nexus Operation input to a Workflow that takes two arguments. */
@ServiceImpl(service = SampleNexusService.class)
public class SampleNexusServiceImpl {

  @TemporalOperation
  public TemporalOperationResult<SampleNexusService.EchoOutput> echo(
      TemporalOperationStartContext ctx,
      TemporalNexusClient client,
      SampleNexusService.EchoInput input) {
    return TemporalOperationResult.sync(new SampleNexusService.EchoOutput(input.getMessage()));
  }

  // @@@SNIPSTART java-nexus-feature-guide-multiple-args
  @TemporalOperation
  public TemporalOperationResult<SampleNexusService.HelloOutput> hello(
      TemporalOperationStartContext ctx,
      TemporalNexusClient client,
      SampleNexusService.HelloInput input) {
    return client.startWorkflow(
        HelloHandlerWorkflow.class,
        HelloHandlerWorkflow::hello,
        input.getName(),
        input.getLanguage(),
        WorkflowOptions.newBuilder().setWorkflowId("hello-" + input.getName()).build());
  }
  // @@@SNIPEND
}
