package io.temporal.docs.workflows.cancellation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.client.ActivityCompletionException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.docs.workflows.cancellation.HelloCancellationScope.GreetingActivities;
import io.temporal.docs.workflows.cancellation.HelloCancellationScope.GreetingWorkflow;
import io.temporal.failure.CanceledFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CancellationTest {

    private static final String TASK_QUEUE = "cancellation";

    private TestWorkflowEnvironment env;
    private Worker worker;
    private WorkflowClient client;

    /** Completes "Hello" right away; every other greeting heartbeats until it's canceled. */
    static class OneFastActivity implements GreetingActivities {
        final AtomicInteger canceled = new AtomicInteger();
        final boolean completeHello;

        OneFastActivity(boolean completeHello) {
            this.completeHello = completeHello;
        }

        @Override
        public String composeGreeting(String greeting, String name) {
            if (completeHello && greeting.equals("Hello")) {
                return greeting + " " + name + "!";
            }
            ActivityExecutionContext context = Activity.getExecutionContext();
            while (true) {
                try {
                    Thread.sleep(200);
                    context.heartbeat(null);
                } catch (ActivityCompletionException e) {
                    canceled.incrementAndGet();
                    throw e;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @BeforeEach
    public void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(HelloCancellationScope.GreetingWorkflowImpl.class);
        client = env.getWorkflowClient();
    }

    @AfterEach
    public void tearDown() {
        env.close();
    }

    private GreetingWorkflow stub(String workflowId) {
        return client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build());
    }

    @Test
    public void cancelsRemainingActivitiesAfterFirstResult() {
        OneFastActivity activities = new OneFastActivity(true);
        worker.registerActivitiesImplementations(activities);
        env.start();

        assertEquals("Hello World!", stub("cancel-scope").getGreeting("World"));
        assertEquals(5, activities.canceled.get());
    }

    @Test
    public void cancelWorkflow() {
        worker.registerActivitiesImplementations(new OneFastActivity(false));
        env.start();

        GreetingWorkflow workflow = stub("cancel-workflow");
        WorkflowClient.start(workflow::getGreeting, "World");
        InterruptWorkflowExample.cancel(workflow);

        WorkflowFailedException e = assertThrows(WorkflowFailedException.class,
            () -> WorkflowStub.fromTyped(workflow).getResult(String.class));
        assertTrue(e.getCause() instanceof CanceledFailure);
    }

    @Test
    public void terminateWorkflow() {
        worker.registerActivitiesImplementations(new OneFastActivity(false));
        env.start();

        GreetingWorkflow workflow = stub("terminate-workflow");
        WorkflowClient.start(workflow::getGreeting, "World");
        InterruptWorkflowExample.terminate(workflow);

        assertThrows(WorkflowFailedException.class,
            () -> WorkflowStub.fromTyped(workflow).getResult(String.class));
        assertEquals(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_TERMINATED,
            client.newUntypedWorkflowStub("terminate-workflow").describe().getWorkflowExecutionInfo().getStatus());
    }
}
