package io.temporal.docs.activities.timeouts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.docs.activities.timeouts.YourActivityDefinition.YourActivityMethodParam;
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

public class ActivityTimeoutsTest {

    private static final String TASK_QUEUE = "activity-timeouts";
    private static final ActivityOptions OPTIONS = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(10))
        .setHeartbeatTimeout(Duration.ofSeconds(5))
        .build();

    @WorkflowInterface
    public interface CallerWorkflow {
        @WorkflowMethod
        String run(String example);
    }

    public static class CallerWorkflowImpl implements CallerWorkflow {
        @Override
        public String run(String example) {
            if (example.equals("delay")) {
                return Workflow.newActivityStub(NextRetryDelayActivities.FlakyActivities.class, OPTIONS)
                    .callService(3);
            }
            return Workflow.newActivityStub(YourActivityDefinition.class, OPTIONS)
                .yourActivityMethod(new YourActivityMethodParam(List.of("a", "b", "c", "d")));
        }
    }

    /** Fails once, after processing "b", so the retry has heartbeat details to resume from. */
    static class FailsOnceImpl extends YourActivityDefinitionImpl {
        final List<String> seen = Collections.synchronizedList(new ArrayList<>());
        private boolean failed;

        @Override
        protected String processItem(String item) {
            seen.add(item);
            if (item.equals("c") && !failed) {
                failed = true;
                throw new IllegalStateException("transient failure");
            }
            return super.processItem(item);
        }
    }

    private TestWorkflowEnvironment env;
    private Worker worker;

    @BeforeEach
    public void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(CallerWorkflowImpl.class);
    }

    @AfterEach
    public void tearDown() {
        env.close();
    }

    private String run(String example) {
        env.start();
        return env.getWorkflowClient().newWorkflowStub(CallerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(example).build())
            .run(example);
    }

    @Test
    public void nextRetryDelayOverridesRetryInterval() {
        worker.registerActivitiesImplementations(new NextRetryDelayActivities.FlakyActivitiesImpl());
        long start = env.currentTimeMillis();
        assertEquals("succeeded on attempt 3", run("delay"));
        // Attempt 1 asks for a 3-second delay and attempt 2 for 6 seconds.
        long elapsed = env.currentTimeMillis() - start;
        assertTrue(elapsed >= 9_000, "elapsed " + elapsed);
    }

    @Test
    public void retryResumesFromLastHeartbeatDetails() {
        FailsOnceImpl activities = new FailsOnceImpl();
        worker.registerActivitiesImplementations(activities);
        assertEquals("attempt 2 processed [C, D]", run("heartbeat"));
        // The first attempt reported "b" before failing on "c", so "a" and "b" aren't processed again.
        assertEquals(List.of("a", "b", "c", "c", "d"), activities.seen);
    }
}
