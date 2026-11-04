package helloworkflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.NexusServiceOptions;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs the caller Workflow against the Nexus handler through an Endpoint on the test server. */
public class NexusQuickstartTest {

    private TestWorkflowEnvironment testEnv;

    @BeforeEach
    public void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();

        Worker handler = testEnv.newWorker("my-task-queue");
        handler.registerWorkflowImplementationTypes(SayHelloWorkflowImpl.class);
        handler.registerActivitiesImplementations(new GreetActivitiesImpl());
        handler.registerNexusServiceImplementation(new SayHelloNexusServiceImpl());
        testEnv.createNexusEndpoint("my-nexus-endpoint-name", "my-task-queue");

        Worker caller = testEnv.newWorker("my-caller-task-queue");
        caller.registerWorkflowImplementationTypes(
            WorkflowImplementationOptions.newBuilder()
                .setNexusServiceOptions(
                    Collections.singletonMap(
                        "SayHelloNexusService",
                        NexusServiceOptions.newBuilder().setEndpoint("my-nexus-endpoint-name").build()))
                .build(),
            NexusCallerWorkflowImpl.class);

        testEnv.start();
    }

    @AfterEach
    public void tearDown() {
        testEnv.close();
    }

    @Test
    public void callerGetsGreetingThroughNexus() {
        NexusCallerWorkflow workflow =
            testEnv
                .getWorkflowClient()
                .newWorkflowStub(
                    NexusCallerWorkflow.class,
                    WorkflowOptions.newBuilder().setTaskQueue("my-caller-task-queue").build());
        assertEquals("Hello Temporal", workflow.greetThroughNexus("Temporal"));
    }
}
