package io.temporal.docs.workers.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InterceptorsTest {

    private static final String TASK_QUEUE = "interceptors";

    @ActivityInterface
    public interface GreetingActivities {
        String composeGreeting(String name);
    }

    public static class GreetingActivitiesImpl implements GreetingActivities {
        @Override
        public String composeGreeting(String name) {
            return "Hello " + name + "!";
        }
    }

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String greet(String name);
    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        @Override
        public String greet(String name) {
            return Workflow.newActivityStub(
                    GreetingActivities.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build())
                .composeGreeting(name);
        }
    }

    private TestWorkflowEnvironment testEnv;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(out, true));
        // slf4j-simple writes Workflow.getLogger() output to System.err
        System.setErr(new PrintStream(err, true));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        testEnv.close();
    }

    @Test
    public void interceptorsForwardCalls() {
        WorkflowClient client = RegisterInterceptors.newClient(testEnv.getWorkflowServiceStubs());
        WorkerFactory factory = RegisterInterceptors.newWorkerFactory(client);
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        factory.start();

        GreetingWorkflow workflow =
            client.newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("interceptors-test")
                    .setTaskQueue(TASK_QUEUE)
                    .build());
        assertEquals("Hello World!", workflow.greet("World"));
        assertTrue(out.toString().contains("starting workflow interceptors-test"));
        assertTrue(err.toString().contains("starting activity ComposeGreeting"), err.toString());

        factory.shutdown();
    }
}
