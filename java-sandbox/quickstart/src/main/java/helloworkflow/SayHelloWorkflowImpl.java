// @@@SNIPSTART java-quickstart-workflow-impl
package helloworkflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class SayHelloWorkflowImpl implements SayHelloWorkflow {

    private final GreetActivities activities = Workflow.newActivityStub(
      GreetActivities.class,
      ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(5))
        .build()
    );

    @Override
    public String sayHello(String name) {
      return activities.greet(name);
    }

}
// @@@SNIPEND
