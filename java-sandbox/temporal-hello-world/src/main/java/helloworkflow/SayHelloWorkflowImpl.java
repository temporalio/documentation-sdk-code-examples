package helloworkflow;

import java.time.Duration;

import io.temporal.activity.ActivityCancellationType;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Workflow;

public class SayHelloWorkflowImpl implements SayHelloWorkflow {

    private CancellationScope scope;

    private final GreetActivities activities = Workflow.newActivityStub(
      GreetActivities.class,
      ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(30))
        .setHeartbeatTimeout(Duration.ofSeconds(2))
        .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
        .build()
    );

    @Override
    public String sayHello(String name) {
      scope = Workflow.newCancellationScope(() -> activities.greet(name));

      scope.run();

      return activities.greet(name);
    }

    @Override
    public void cancelActivity() {
      if (scope != null) {
        scope.cancel("canceled by test");
      }
    }

}