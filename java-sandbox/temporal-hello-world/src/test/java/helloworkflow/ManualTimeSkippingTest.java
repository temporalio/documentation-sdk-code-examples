package helloworkflow;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class ManualTimeSkippingTest {
  private static final String TASK_QUEUE = "manual-time-skipping-test";

  @WorkflowInterface
  public interface ProgressWorkflow {
    @WorkflowMethod
    void run();

    @QueryMethod
    int daysElapsed();
  }

  public static class ProgressWorkflowImpl implements ProgressWorkflow {
    private int daysElapsed = 0;

    @Override
    public void run() {
      for (int i = 0; i < 100; i++) {
        Workflow.sleep(Duration.ofDays(1));
        daysElapsed++;
      }
    }

    @Override
    public int daysElapsed() {
      return daysElapsed;
    }
  }

  @Test
  void manuallyAdvanceWorkflowTime() {
    try (TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance()) {
      Worker worker = testEnv.newWorker(TASK_QUEUE);
      worker.registerWorkflowImplementationTypes(ProgressWorkflowImpl.class);
      testEnv.start();

      ProgressWorkflow workflow =
          testEnv
              .getWorkflowClient()
              .newWorkflowStub(
                  ProgressWorkflow.class,
                  WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

      WorkflowClient.start(workflow::run);

      assertEquals(0, workflow.daysElapsed());

      testEnv.sleep(Duration.ofHours(25));
      assertEquals(1, workflow.daysElapsed());

      testEnv.sleep(Duration.ofHours(25));
      assertEquals(2, workflow.daysElapsed());
    }
  }
}