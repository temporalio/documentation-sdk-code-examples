package io.temporal.docs.workflows.childworkflows;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ChildWorkflowsTest {

    private static final String TASK_QUEUE = "child-workflows";

    private TestWorkflowEnvironment env;
    private Worker worker;
    private WorkflowClient client;

    @BeforeEach
    public void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        worker = env.newWorker(TASK_QUEUE);
        client = env.getWorkflowClient();
    }

    @AfterEach
    public void tearDown() {
        env.close();
    }

    private <T> T stub(Class<T> type, String workflowId) {
        return client.newWorkflowStub(
            type,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(workflowId)
                .build());
    }

    @Test
    public void asyncChild() {
        worker.registerWorkflowImplementationTypes(
            AsyncChildExample.GreetingWorkflowImpl.class, AsyncChildExample.GreetingChildImpl.class);
        env.start();

        assertEquals("Hello Temporal!",
            stub(AsyncChildExample.GreetingWorkflow.class, "async").getGreeting("Temporal"));
    }

    @Test
    public void asyncUntypedChild() {
        worker.registerWorkflowImplementationTypes(
            AsyncChildExample.UntypedGreetingWorkflowImpl.class,
            AsyncChildExample.GreetingChildImpl.class);
        env.start();

        assertEquals("Hello Temporal!",
            stub(AsyncChildExample.GreetingWorkflow.class, "async-untyped").getGreeting("Temporal"));
    }

    @Test
    public void spawnChild() {
        worker.registerWorkflowImplementationTypes(
            SpawnChildExample.GreetingWorkflowImpl.class, SpawnChildExample.GreetingChildImpl.class);
        env.start();

        assertEquals("Hello Temporal!",
            stub(SpawnChildExample.GreetingWorkflow.class, "spawn").getGreeting("Temporal"));
    }

    @Test
    public void parallelChildren() {
        worker.registerWorkflowImplementationTypes(
            ParallelChildrenExample.GreetingWorkflowImpl.class,
            ParallelChildrenExample.GreetingChildImpl.class);
        env.start();

        assertEquals("First: Hello Temporal!, second: Bye Temporal!",
            stub(ParallelChildrenExample.GreetingWorkflow.class, "parallel").getGreeting("Temporal"));
    }

    @Test
    public void signalChild() {
        worker.registerWorkflowImplementationTypes(
            SignalChildExample.GreetingWorkflowImpl.class, SignalChildExample.GreetingChildImpl.class);
        env.start();

        assertEquals("Hello Temporal!",
            stub(SignalChildExample.GreetingWorkflow.class, "signal").getGreeting("World"));
    }

    @Test
    public void abandonedChildKeepsRunningAfterParentCompletes() {
        worker.registerWorkflowImplementationTypes(
            ParentClosePolicyExample.ParentWorkflowImpl.class,
            ParentClosePolicyExample.MyChildWorkflowImpl.class);
        env.start();

        stub(ParentClosePolicyExample.ParentWorkflow.class, "parent").parentWorkflow();

        WorkflowExecution childExecution = client.fetchHistory("parent").getEvents().stream()
            .filter(HistoryEvent::hasChildWorkflowExecutionStartedEventAttributes)
            .map(e -> e.getChildWorkflowExecutionStartedEventAttributes().getWorkflowExecution())
            .findFirst()
            .orElseThrow();
        WorkflowExecutionStatus status = env.getWorkflowServiceStubs().blockingStub()
            .describeWorkflowExecution(DescribeWorkflowExecutionRequest.newBuilder()
                .setNamespace(env.getNamespace())
                .setExecution(childExecution)
                .build())
            .getWorkflowExecutionInfo()
            .getStatus();
        assertEquals(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING, status);
    }
}
