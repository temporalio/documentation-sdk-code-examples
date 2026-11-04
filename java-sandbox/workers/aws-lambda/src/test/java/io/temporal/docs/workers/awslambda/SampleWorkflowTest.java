package io.temporal.docs.workers.awslambda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit test for the sample Workflow and Activity. */
public class SampleWorkflowTest {

  private static final String TASK_QUEUE = "aws-lambda";

  private TestWorkflowEnvironment testEnv;

  @BeforeEach
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    Worker worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(SampleWorkflowImpl.class);
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    testEnv.start();
  }

  @AfterEach
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void workflowReturnsGreeting() {
    SampleWorkflow workflow =
        testEnv
            .getWorkflowClient()
            .newWorkflowStub(
                SampleWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

    assertEquals(
        "Hello, Serverless Lambda Worker!!", workflow.getGreeting("Serverless Lambda Worker!"));
  }
}
