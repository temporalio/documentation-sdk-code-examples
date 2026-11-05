package io.temporal.docs.bestpractices.testingsuite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;

public class SkipTimeTest {
  private static final String TASK_QUEUE = "skip-time";

  // @@@SNIPSTART java-testing-suite-skip-time-automatically
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
  // @@@SNIPEND
}
