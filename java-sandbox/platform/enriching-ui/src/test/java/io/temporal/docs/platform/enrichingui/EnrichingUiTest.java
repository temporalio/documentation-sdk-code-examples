package io.temporal.docs.platform.enrichingui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.api.common.v1.Payload;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.sdk.v1.WorkflowMetadata;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionDescription;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EnrichingUiTest {

    private static final String TASK_QUEUE = "your-task-queue";

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;
    private Worker worker;

    @BeforeEach
    public void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        client = testEnv.getWorkflowClient();
        worker = testEnv.newWorker(TASK_QUEUE);
    }

    @AfterEach
    public void tearDown() {
        testEnv.close();
    }

    private static String decode(Payload payload) {
        return DefaultDataConverter.STANDARD_INSTANCE.fromPayload(payload, String.class, String.class);
    }

    private HistoryEvent firstEvent(String workflowId, EventType type) {
        return client.fetchHistory(workflowId).getEvents().stream()
            .filter(e -> e.getEventType() == type)
            .findFirst()
            .orElseThrow();
    }

    private YourWorkflow stub(String workflowId) {
        return client.newWorkflowStub(YourWorkflow.class,
            WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(TASK_QUEUE).build());
    }

    @Test
    public void staticSummaryAndDetailsAreSetAtStart() {
        worker.registerWorkflowImplementationTypes(CurrentDetailsWorkflowImpl.class);
        testEnv.start();

        assertEquals("Workflow completed", Main.runWorkflow(client));
        WorkflowExecutionDescription description =
            client.newUntypedWorkflowStub("your-workflow-id").describe();
        assertEquals("Order processing for customer #12345", description.getStaticSummary());
        assertEquals("Processing premium order with expedited shipping", description.getStaticDetails());
    }

    @Test
    public void asyncStartKeepsStaticSummary() {
        worker.registerWorkflowImplementationTypes(CurrentDetailsWorkflowImpl.class);
        testEnv.start();

        WorkflowExecution execution = AsyncStart.startWorkflow(client);
        WorkflowStub workflow = client.newUntypedWorkflowStub(execution.getWorkflowId());
        assertEquals("Workflow completed", workflow.getResult(String.class));
        assertEquals("Order processing for customer #12345", workflow.describe().getStaticSummary());
    }

    @Test
    public void currentDetailsAreReturnedByTheMetadataQuery() {
        worker.registerWorkflowImplementationTypes(CurrentDetailsWorkflowImpl.class);
        testEnv.start();

        YourWorkflow workflow = stub("current-details");
        assertEquals("Workflow completed", workflow.yourWorkflowMethod("input"));
        WorkflowMetadata metadata = WorkflowStub.fromTyped(workflow)
            .query("__temporal_workflow_metadata", WorkflowMetadata.class);
        assertEquals("Updated workflow details with new status", metadata.getCurrentDetails());
    }

    @Test
    public void activitySummaryIsRecordedInHistory() {
        worker.registerWorkflowImplementationTypes(ActivitySummaryWorkflowImpl.class);
        worker.registerActivitiesImplementations(new YourActivitiesImpl());
        testEnv.start();

        assertEquals("Processed input", stub("activity-summary").yourWorkflowMethod("input"));
        HistoryEvent scheduled =
            firstEvent("activity-summary", EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED);
        assertEquals("Processing user data", decode(scheduled.getUserMetadata().getSummary()));
    }

    @Test
    public void timerSummaryIsRecordedInHistory() {
        worker.registerWorkflowImplementationTypes(TimerSummaryWorkflowImpl.class);
        testEnv.start();

        // The test server skips time, so the 5-minute Timer fires right away.
        assertEquals("Timer completed", stub("timer-summary").yourWorkflowMethod("input"));
        HistoryEvent started = firstEvent("timer-summary", EventType.EVENT_TYPE_TIMER_STARTED);
        assertEquals("Waiting for payment confirmation", decode(started.getUserMetadata().getSummary()));
    }
}
