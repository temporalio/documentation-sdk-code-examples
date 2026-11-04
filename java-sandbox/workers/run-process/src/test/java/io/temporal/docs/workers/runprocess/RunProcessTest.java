package io.temporal.docs.workers.runprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.EncodedValues;
import io.temporal.docs.workers.runprocess.RegisterTypesExample.DatabaseClient;
import io.temporal.docs.workers.runprocess.RegisterTypesExample.GreetingWorkflow;
import io.temporal.docs.workers.runprocess.RegisterTypesExample.GreetingWorkflowImpl;
import io.temporal.docs.workers.runprocess.RegisterTypesExample.OrderWorkflow;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.DynamicWorkflow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RunProcessTest {

    private static final String TASK_QUEUE = "run-process";

    private TestWorkflowEnvironment testEnv;
    private Worker worker;

    @BeforeEach
    public void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(TASK_QUEUE);
    }

    @AfterEach
    public void tearDown() {
        testEnv.close();
    }

    @Test
    public void registeredTypesRun() {
        RegisterTypesExample.registerTypes(worker, new DatabaseClient());
        testEnv.start();
        WorkflowClient client = testEnv.getWorkflowClient();
        WorkflowOptions options = WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();

        assertEquals("Hello World!", client.newWorkflowStub(GreetingWorkflow.class, options).greet("World"));
        assertEquals("Placed order 42", client.newWorkflowStub(OrderWorkflow.class, options).placeOrder("42"));
    }

    /** A second implementation of the GreetingWorkflow type. */
    public static class OtherGreetingWorkflowImpl implements GreetingWorkflow {
        @Override
        public String greet(String name) {
            return "Hi " + name;
        }
    }

    @Test
    public void registeringSameWorkflowTypeTwiceThrows() {
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        assertThrows(
            IllegalStateException.class,
            () -> worker.registerWorkflowImplementationTypes(OtherGreetingWorkflowImpl.class));
    }

    public static class DynamicOne implements DynamicWorkflow {
        @Override
        public Object execute(EncodedValues args) {
            return "one";
        }
    }

    public static class DynamicTwo implements DynamicWorkflow {
        @Override
        public Object execute(EncodedValues args) {
            return "two";
        }
    }

    @Test
    public void onlyOneDynamicWorkflowPerWorker() {
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, DynamicOne.class);
        assertThrows(
            IllegalStateException.class,
            () -> worker.registerWorkflowImplementationTypes(DynamicTwo.class));
    }
}
