package io.temporal.docs.bestpractices.testingsuite;

import io.temporal.activity.ActivityCancellationType;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Workflow;
import java.time.Duration;

// @@@SNIPSTART java-testing-suite-cancellable-workflow
public class CancellableGreetingWorkflowImpl implements CancellableGreetingWorkflow {
  private CancellationScope scope;

  private final GreetActivities activities =
      Workflow.newActivityStub(
          GreetActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              // The Activity learns about the cancellation on its next Heartbeat.
              .setHeartbeatTimeout(Duration.ofSeconds(2))
              // Wait for the Activity to acknowledge the cancellation before failing.
              .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
              .build());

  @Override
  public String sayHello(String name) {
    scope = Workflow.newCancellationScope(() -> activities.greet(name));
    scope.run();
    return "Activity completed without being canceled";
  }

  @Override
  public void cancelActivity() {
    if (scope != null) {
      scope.cancel("canceled by test");
    }
  }
}
// @@@SNIPEND
