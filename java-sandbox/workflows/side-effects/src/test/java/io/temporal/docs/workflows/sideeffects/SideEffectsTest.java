package io.temporal.docs.workflows.sideeffects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.api.enums.v1.EventType;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.WorkflowReplayer;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SideEffectsTest {

    private static final String TASK_QUEUE = "side-effects";

    private TestWorkflowEnvironment env;

    @BeforeEach
    public void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        Worker worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(
            SideEffectsExample.SideEffectWorkflowImpl.class,
            SideEffectsExample.SecureRandomWorkflowImpl.class,
            SideEffectsExample.NewRandomWorkflowImpl.class,
            SideEffectsExample.RandomUuidWorkflowImpl.class);
        env.start();
    }

    @AfterEach
    public void tearDown() {
        env.close();
    }

    /** Runs the Workflow, then replays its history; replay fails if the results aren't deterministic. */
    private WorkflowExecutionHistory runAndReplay(String workflowType, Class<?> implementation) throws Exception {
        WorkflowStub stub = env.getWorkflowClient().newUntypedWorkflowStub(workflowType,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowType).build());
        stub.start();
        stub.getResult(Void.class);
        WorkflowExecutionHistory history = env.getWorkflowClient().fetchHistory(workflowType);
        WorkflowReplayer.replayWorkflowExecution(history, implementation);
        return history;
    }

    private long markers(WorkflowExecutionHistory history) {
        return history.getEvents().stream()
            .filter(e -> e.getEventType() == EventType.EVENT_TYPE_MARKER_RECORDED)
            .count();
    }

    @Test
    public void sideEffectRecordsResult() throws Exception {
        assertEquals(1, markers(runAndReplay("SideEffectWorkflow", SideEffectsExample.SideEffectWorkflowImpl.class)));
    }

    @Test
    public void secureRandomSideEffects() throws Exception {
        assertEquals(2, markers(runAndReplay("SecureRandomWorkflow", SideEffectsExample.SecureRandomWorkflowImpl.class)));
    }

    @Test
    public void newRandomIsDeterministicWithoutSideEffect() throws Exception {
        assertEquals(0, markers(runAndReplay("NewRandomWorkflow", SideEffectsExample.NewRandomWorkflowImpl.class)));
    }

    @Test
    public void randomUuidIsDeterministicWithoutSideEffect() throws Exception {
        assertEquals(0, markers(runAndReplay("RandomUuidWorkflow", SideEffectsExample.RandomUuidWorkflowImpl.class)));
    }
}
