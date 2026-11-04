package io.temporal.docs.nexus.featureguide.handler;

import io.nexusrpc.handler.ServiceImpl;
import io.temporal.client.WorkflowOptions;
import io.temporal.docs.nexus.featureguide.service.SampleNexusService;
import io.temporal.nexus.TemporalNexusClient;
import io.temporal.nexus.TemporalOperation;
import io.temporal.nexus.TemporalOperationResult;
import io.temporal.nexus.TemporalOperationStartContext;
import java.util.Locale;

// @@@SNIPSTART java-nexus-feature-guide-service-impl
@ServiceImpl(service = SampleNexusService.class)
public class SampleNexusServiceImpl {

  @TemporalOperation
  public TemporalOperationResult<SampleNexusService.EchoOutput> echo(
      TemporalOperationStartContext ctx,
      TemporalNexusClient client,
      SampleNexusService.EchoInput input) {
    return TemporalOperationResult.sync(new SampleNexusService.EchoOutput(input.getMessage()));
  }

  @TemporalOperation
  public TemporalOperationResult<SampleNexusService.HelloOutput> hello(
      TemporalOperationStartContext ctx,
      TemporalNexusClient client,
      SampleNexusService.HelloInput input) {
    return client.startWorkflow(
        HelloHandlerWorkflow.class,
        HelloHandlerWorkflow::hello,
        input,
        WorkflowOptions.newBuilder()
            .setWorkflowId(
                String.format(
                    "hello-%s-%s",
                    input.getName(), input.getLanguage().name().toLowerCase(Locale.ROOT)))
            .build());
  }
}
// @@@SNIPEND
