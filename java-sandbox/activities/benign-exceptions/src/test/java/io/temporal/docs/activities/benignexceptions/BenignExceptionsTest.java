package io.temporal.docs.activities.benignexceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BenignExceptionsTest {

    private static final String TASK_QUEUE = "benign-exceptions";

    @WorkflowInterface
    public interface CallerWorkflow {
        @WorkflowMethod
        String run();
    }

    /** Catches the Activity failure and reports what the Workflow received. */
    public static class CallerWorkflowImpl implements CallerWorkflow {
        @Override
        public String run() {
            MyActivities activities =
                Workflow.newActivityStub(
                    MyActivities.class,
                    ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(10))
                        .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                        .build());
            try {
                return activities.myActivity();
            } catch (ActivityFailure e) {
                ApplicationFailure cause = (ApplicationFailure) e.getCause();
                return cause.getCategory() + " " + cause.getType() + ": " + cause.getOriginalMessage();
            }
        }
    }

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;

    @BeforeEach
    public void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        client = testEnv.getWorkflowClient();
        Worker worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(CallerWorkflowImpl.class);
        worker.registerActivitiesImplementations(new MyActivitiesImpl());
        testEnv.start();
    }

    @AfterEach
    public void tearDown() {
        testEnv.close();
    }

    @Test
    public void workflowReceivesBenignFailure() {
        CallerWorkflow workflow =
            client.newWorkflowStub(
                CallerWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
        assertEquals(
            "BENIGN java.lang.IllegalStateException: Service isn't ready yet", workflow.run());
    }
}
