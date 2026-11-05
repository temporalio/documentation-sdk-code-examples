package io.temporal.docs.bestpractices.testingsuite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class CancelActivityTest {
  private static final String TASK_QUEUE = "say-hello";

  // @@@SNIPSTART java-testing-suite-cancel-activity
  @Test
  void testCancelActivity() {
    try (TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance()) {
      Worker worker = env.newWorker(TASK_QUEUE);

      worker.registerWorkflowImplementationTypes(CancellableGreetingWorkflowImpl.class);
      worker.registerActivitiesImplementations(new GreetActivitiesImpl());

      env.start();

      CancellableGreetingWorkflow workflow =
          env.getWorkflowClient()
              .newWorkflowStub(
                  CancellableGreetingWorkflow.class,
                  WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

      WorkflowClient.start(workflow::sayHello, "Temporal");

      env.registerDelayedCallback(
          Duration.ofSeconds(1),
          () -> WorkflowStub.fromTyped(workflow).signal("cancelActivity"));

      try {
        WorkflowStub.fromTyped(workflow).getResult(String.class);
        fail("Workflow should have failed because the Activity was canceled");
      } catch (WorkflowFailedException e) {
        assertEquals(ActivityFailure.class, e.getCause().getClass());

        ActivityFailure activityFailure = (ActivityFailure) e.getCause();
        assertEquals(CanceledFailure.class, activityFailure.getCause().getClass());
      }
    }
  }
  // @@@SNIPEND
}
