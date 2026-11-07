package io.temporal.docs.bestpractices.testingsuite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;

public class MockedActivityTest {
  private static final String TASK_QUEUE = "mocked-activity";

  // @@@SNIPSTART java-testing-suite-mock-activity
  @Test
  public void testMockedActivity() {
    try (TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance()) {
      // withoutAnnotations() keeps Mockito from copying @ActivityInterface onto the mock class.
      GreetActivities activities =
          mock(GreetActivities.class, withSettings().withoutAnnotations());
      when(activities.greet("Temporal")).thenReturn("Hello from the mock!");

      Worker worker = testEnv.newWorker(TASK_QUEUE);
      worker.registerWorkflowImplementationTypes(SayHelloWorkflowImpl.class);
      worker.registerActivitiesImplementations(activities);
      testEnv.start();

      SayHelloWorkflow workflow =
          testEnv
              .getWorkflowClient()
              .newWorkflowStub(
                  SayHelloWorkflow.class,
                  WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

      assertEquals("Hello from the mock!", workflow.sayHello("Temporal"));
    }
  }
  // @@@SNIPEND
}
