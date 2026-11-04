package io.temporal.docs.workers.cloudrun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GreetingWorkflowTest {
  private static final String TASK_QUEUE = "cloud-run";

  private TestWorkflowEnvironment testEnv;

  @BeforeEach
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
  }

  @AfterEach
  public void tearDown() {
    testEnv.close();
  }

  private String run(Class<?> workflowType) {
    Worker worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(workflowType);
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    testEnv.start();

    GreetingWorkflow workflow =
        testEnv
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    return workflow.getGreeting("Google Cloud");
  }

  @Test
  public void completesGreeting() {
    assertEquals("Hello Google Cloud!", run(GreetingWorkflowImpl.class));
  }

  @Test
  public void pinnedWorkflowCompletesGreeting() {
    assertEquals(
        "Hello Google Cloud!",
        run(io.temporal.docs.workers.cloudrun.perworkflow.GreetingWorkflowImpl.class));
  }
}
