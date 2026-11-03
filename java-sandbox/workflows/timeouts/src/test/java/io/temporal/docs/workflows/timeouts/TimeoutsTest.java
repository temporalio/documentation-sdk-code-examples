package io.temporal.docs.workflows.timeouts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.api.enums.v1.TimeoutType;
import io.temporal.client.WorkflowFailedException;
import io.temporal.failure.TimeoutFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TimeoutsTest {

    private TestWorkflowEnvironment env;

    @BeforeEach
    public void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        Worker worker = env.newWorker(TimeoutsExample.TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(GreetWorkflowImpl.class);
        env.start();
    }

    @AfterEach
    public void tearDown() {
        env.close();
    }

    @Test
    public void executionTimeoutFailsLongWorkflow() {
        GreetWorkflowInterface workflow = TimeoutsExample.withExecutionTimeout(env.getWorkflowClient());

        WorkflowFailedException e = assertThrows(WorkflowFailedException.class, () -> workflow.greet("slow"));
        assertTrue(e.getCause() instanceof TimeoutFailure);
        assertEquals(TimeoutType.TIMEOUT_TYPE_START_TO_CLOSE, ((TimeoutFailure) e.getCause()).getTimeoutType());
    }

    @Test
    public void executionTimeoutAllowsFastWorkflow() {
        assertEquals("Hello Temporal!",
            TimeoutsExample.withExecutionTimeout(env.getWorkflowClient()).greet("Temporal"));
    }

    @Test
    public void retryPolicyRetriesFailedWorkflow() {
        assertEquals("Hello flaky!",
            TimeoutsExample.withRetryOptions(env.getWorkflowClient()).greet("flaky"));
    }
}
