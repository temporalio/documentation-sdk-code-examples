package io.temporal.docs.workers.runprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.docs.workers.runprocess.RegisterTypesExample.GreetingWorkflow;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.WorkerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WorkerExamplesTest {

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;

    @BeforeEach
    public void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        client = testEnv.getWorkflowClient();
    }

    @AfterEach
    public void tearDown() {
        testEnv.close();
    }

    @Test
    public void createRunAndShutDownWorker() {
        WorkerFactory factory = WorkerExamples.createWorker(client);
        GreetingWorkflow workflow =
            client.newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue("my-task-queue").build());
        assertEquals("Hello World!", workflow.greet("World"));

        WorkerExamples.shutDown(factory);
        assertTrue(factory.isTerminated());
    }

    @Test
    public void createVersionedWorker() {
        WorkerFactory factory = WorkerFactory.newInstance(client);
        assertEquals("my-task-queue", WorkerExamples.createVersionedWorker(factory).getTaskQueue());
    }

    @Test
    public void createWorkerThatHeartbeatsDuringShutdown() {
        WorkerFactory factory = WorkerFactory.newInstance(client);
        assertEquals(
            "your-task-queue",
            WorkerExamples.newWorkerThatHeartbeatsDuringShutdown(factory).getTaskQueue());
    }
}
