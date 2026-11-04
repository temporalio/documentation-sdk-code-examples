package io.temporal.docs.nexus.featureguide.messaging;

import io.nexusrpc.handler.ServiceImpl;
import io.temporal.docs.nexus.featureguide.messaging.GreetingNexusService.ApproveInput;
import io.temporal.docs.nexus.featureguide.messaging.GreetingNexusService.ApproveOutput;
import io.temporal.nexus.TemporalNexusClient;
import io.temporal.nexus.TemporalOperation;
import io.temporal.nexus.TemporalOperationResult;
import io.temporal.nexus.TemporalOperationStartContext;

/** Signals a running Workflow whose ID is derived from the user identifier in the input. */
@ServiceImpl(service = GreetingNexusService.class)
public class GreetingNexusServiceImpl {

  // @@@SNIPSTART java-nexus-feature-guide-signal
  @TemporalOperation
  public TemporalOperationResult<ApproveOutput> approve(
      TemporalOperationStartContext ctx, TemporalNexusClient client, ApproveInput input) {
    client
        .getWorkflowClient()
        .newWorkflowStub(GreetingWorkflow.class, "GreetingWorkflow_for_" + input.getUserId())
        .approve(input);
    return TemporalOperationResult.sync(new ApproveOutput());
  }
  // @@@SNIPEND
}
