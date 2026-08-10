package helloworkflow;


import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.testing.TestActivityEnvironment;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

public class GreetActivitiesTest {

  private static final String TASK_QUEUE = "say-hello";
  // Basic Activity test that directly calls the activity code
  @Test
  public void testActivityImpl() {
    TestActivityEnvironment testEnv = TestActivityEnvironment.newInstance();

    testEnv.registerActivitiesImplementations(new GreetActivitiesImpl());

    GreetActivities activities = testEnv.newActivityStub(GreetActivities.class);

    String result = activities.greet("Temporal");

    assertEquals("Hello Temporal!", result);
  }

  @Test
  void testActivityHeartbeat() {
    TestActivityEnvironment env = TestActivityEnvironment.newInstance();

    AtomicInteger heartbeatCount = new AtomicInteger(0);

    env.setActivityHeartbeatListener(
        Void.class,
        heartbeat -> heartbeatCount.incrementAndGet());

    env.registerActivitiesImplementations(new GreetActivitiesImpl());

    GreetActivities activities = env.newActivityStub(GreetActivities.class);

    String result = activities.greet("Temporal");

    assertEquals("Hello Temporal!", result);
    assertEquals(5, heartbeatCount.get());
  }

  @Test
  void testCancelActivity() {
    try (TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance()) {
      Worker worker = env.newWorker(TASK_QUEUE);

      worker.registerWorkflowImplementationTypes(SayHelloWorkflowImpl.class);
      worker.registerActivitiesImplementations(new GreetActivitiesImpl());

      env.start();

      SayHelloWorkflow workflow =
        env.getWorkflowClient()
          .newWorkflowStub(
              SayHelloWorkflow.class,
              WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .build());

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

  @Test
  public void testMockedActivity() {
    GreetActivities activities =
        mock(GreetActivities.class, withSettings().withoutAnnotations());

    when(activities.greet("Temporal")).thenReturn("Hello Temporal!");

    assertEquals("Hello Temporal!", activities.greet("Temporal"));
  }

  @Test
  void testSleepCompletesWithoutWaitingOneDay() {
    try (TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance()) {
      Worker worker = testEnv.newWorker(TASK_QUEUE);
      
      worker.registerWorkflowImplementationTypes(SayHelloWorkflowImpl.class);
      worker.registerActivitiesImplementations(new GreetActivitiesImpl());
      
      testEnv.start();

      SayHelloWorkflow workflow =
          testEnv
              .getWorkflowClient()
              .newWorkflowStub(
                  SayHelloWorkflow.class,
                  WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

      String result = workflow.sayHello("Temporal");

      assertEquals("Hello Temporal!", result);
    }
  }
}