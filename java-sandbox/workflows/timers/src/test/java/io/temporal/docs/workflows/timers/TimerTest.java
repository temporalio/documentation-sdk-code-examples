package io.temporal.docs.workflows.timers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TimerTest {

    private TestWorkflowEnvironment env;

    @BeforeEach
    public void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        Worker worker = env.newWorker("timers");
        worker.registerWorkflowImplementationTypes(TimerExample.TimerWorkflowImpl.class);
        env.start();
    }

    @AfterEach
    public void tearDown() {
        env.close();
    }

    @Test
    public void sleepStartsFiveSecondTimer() {
        TimerExample.TimerWorkflow workflow = env.getWorkflowClient().newWorkflowStub(
            TimerExample.TimerWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue("timers").setWorkflowId("timer").build());

        long start = env.currentTimeMillis();
        assertEquals("Timer fired", workflow.run());
        assertTrue(env.currentTimeMillis() - start >= 5000);

        HistoryEvent timerStarted = env.getWorkflowClient().fetchHistory("timer").getEvents().stream()
            .filter(e -> e.getEventType() == EventType.EVENT_TYPE_TIMER_STARTED)
            .findFirst()
            .orElseThrow();
        assertEquals(5, timerStarted.getTimerStartedEventAttributes().getStartToFireTimeout().getSeconds());
    }
}
