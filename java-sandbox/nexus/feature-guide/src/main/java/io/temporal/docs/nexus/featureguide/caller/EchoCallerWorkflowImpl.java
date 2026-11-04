// @@@SNIPSTART java-nexus-feature-guide-echo-caller-workflow
package io.temporal.docs.nexus.featureguide.caller;

import io.temporal.docs.nexus.featureguide.service.SampleNexusService;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class EchoCallerWorkflowImpl implements EchoCallerWorkflow {
  SampleNexusService sampleNexusService =
      Workflow.newNexusServiceStub(
          SampleNexusService.class,
          NexusServiceOptions.newBuilder()
              .setOperationOptions(
                  NexusOperationOptions.newBuilder()
                      .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                      .build())
              .build());

  @Override
  public String echo(String message) {
    return sampleNexusService.echo(new SampleNexusService.EchoInput(message)).getMessage();
  }
}
// @@@SNIPEND
