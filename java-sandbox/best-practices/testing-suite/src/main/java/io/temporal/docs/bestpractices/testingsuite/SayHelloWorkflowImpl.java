package io.temporal.docs.bestpractices.testingsuite;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

// @@@SNIPSTART java-testing-suite-sleep-workflow
public class SayHelloWorkflowImpl implements SayHelloWorkflow {
  private final GreetActivities activities =
      Workflow.newActivityStub(
          GreetActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              .build());

  @Override
  public String sayHello(String name) {
    // This will complete immediately in TestWorkflowEnvironment because
    // Workflow time is automatically advanced.
    Workflow.sleep(Duration.ofDays(1));

    return activities.greet(name);
  }
}
// @@@SNIPEND
