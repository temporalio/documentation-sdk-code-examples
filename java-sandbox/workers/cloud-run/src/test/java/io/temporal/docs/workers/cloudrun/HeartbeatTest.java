package io.temporal.docs.workers.cloudrun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.docs.workers.cloudrun.heartbeat.ItemActivities;
import io.temporal.docs.workers.cloudrun.heartbeat.ItemActivitiesImpl;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HeartbeatTest {
  private static final String TASK_QUEUE = "cloud-run-heartbeat";

  @WorkflowInterface
  public interface ProcessWorkflow {
    @WorkflowMethod
    String run(List<String> items);
  }

  public static class ProcessWorkflowImpl implements ProcessWorkflow {
    @Override
    public String run(List<String> items) {
      return Workflow.newActivityStub(
              ItemActivities.class,
              ActivityOptions.newBuilder()
                  .setStartToCloseTimeout(Duration.ofSeconds(10))
                  .setRetryOptions(
                      RetryOptions.newBuilder().setInitialInterval(Duration.ofMillis(100)).build())
                  .build())
          .process(items);
    }
  }

  /** Fails once on "c", after "a" and "b" are recorded, as if the instance were stopped. */
  static class FailsOnceImpl extends ItemActivitiesImpl {
    final List<String> seen = Collections.synchronizedList(new ArrayList<>());
    private boolean failed;

    @Override
    protected void processItem(String item) {
      seen.add(item);
      if (item.equals("c") && !failed) {
        failed = true;
        throw new IllegalStateException("instance stopped");
      }
    }
  }

  private TestWorkflowEnvironment testEnv;

  @BeforeEach
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
  }

  @AfterEach
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void retryResumesAfterLastHeartbeat() {
    FailsOnceImpl activities = new FailsOnceImpl();
    Worker worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ProcessWorkflowImpl.class);
    worker.registerActivitiesImplementations(activities);
    testEnv.start();

    ProcessWorkflow workflow =
        testEnv
            .getWorkflowClient()
            .newWorkflowStub(
                ProcessWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    assertEquals("done", workflow.run(List.of("a", "b", "c", "d")));
    // The retry starts at "c" rather than "a".
    assertEquals(List.of("a", "b", "c", "c", "d"), activities.seen);
  }
}
