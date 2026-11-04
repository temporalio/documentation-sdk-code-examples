// @@@SNIPSTART java-nexus-quickstart-caller-workflow-impl
package helloworkflow;

import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class NexusCallerWorkflowImpl implements NexusCallerWorkflow {

    private final SayHelloNexusService nexusService = Workflow.newNexusServiceStub(
        SayHelloNexusService.class,
        NexusServiceOptions.newBuilder()
            .setOperationOptions(
                NexusOperationOptions.newBuilder()
                    .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                    .build())
            .build()
    );

    @Override
    public String greetThroughNexus(String name) {
        return nexusService.sayHello(name);
    }

}
// @@@SNIPEND
