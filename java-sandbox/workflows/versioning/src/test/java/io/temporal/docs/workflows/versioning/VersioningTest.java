package io.temporal.docs.workflows.versioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.api.common.v1.Payload;
import io.temporal.api.enums.v1.EventType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.docs.workflows.versioning.FileProcessing.Arguments;
import io.temporal.docs.workflows.versioning.FileProcessing.FileProcessingActivities;
import io.temporal.docs.workflows.versioning.FileProcessing.FileProcessingWorkflow;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.WorkflowReplayer;
import io.temporal.worker.Worker;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VersioningTest {

    private static final String TASK_QUEUE = "versioning";

    private TestWorkflowEnvironment env;
    private Worker worker;
    private WorkflowClient client;

    static class FakeFileActivities implements FileProcessingActivities {
        @Override
        public String download(String bucketName, String filename) {
            return "/tmp/" + filename;
        }

        @Override
        public String processFile(String localName) {
            return localName + ".processed";
        }

        @Override
        public void upload(String bucketName, String filename, String localName) {}

        @Override
        public long calculateChecksum(String localName) {
            return localName.hashCode();
        }

        @Override
        public void uploadWithChecksum(String bucketName, String filename, String localName, long checksum) {}

        @Override
        public void deleteLocalFile(String localName) {}
    }

    @BeforeEach
    public void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        worker = env.newWorker(TASK_QUEUE);
        worker.registerActivitiesImplementations(new FakeFileActivities());
        client = env.getWorkflowClient();
    }

    @AfterEach
    public void tearDown() {
        env.close();
    }

    private <T> T stub(Class<T> type, String workflowId) {
        return client.newWorkflowStub(
            type, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build());
    }

    private WorkflowExecutionHistory runFileProcessing(String workflowId) {
        stub(FileProcessingWorkflow.class, workflowId)
            .processFile(new Arguments("source", "in.txt", "target", "out.txt"));
        return client.fetchHistory(workflowId);
    }

    private List<String> activityTypes(WorkflowExecutionHistory history) {
        return history.getEvents().stream()
            .filter(e -> e.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED)
            .map(e -> e.getActivityTaskScheduledEventAttributes().getActivityType().getName())
            .collect(Collectors.toList());
    }

    private String changeVersionUpserts(WorkflowExecutionHistory history) {
        return history.getEvents().stream()
            .filter(e -> e.getEventType() == EventType.EVENT_TYPE_UPSERT_WORKFLOW_SEARCH_ATTRIBUTES)
            .map(e -> e.getUpsertWorkflowSearchAttributesEventAttributes().getSearchAttributes()
                .getIndexedFieldsMap().get("TemporalChangeVersion"))
            .map(Payload::getData)
            .map(data -> data.toStringUtf8())
            .collect(Collectors.joining(";"));
    }

    @Test
    public void patchedWorkflowReplaysOriginalHistory() throws Exception {
        worker.registerWorkflowImplementationTypes(FileProcessingWorkflowOriginal.class);
        env.start();

        WorkflowExecutionHistory original = runFileProcessing("original");
        assertTrue(!activityTypes(original).contains("CalculateChecksum"));

        // The patched code takes the DEFAULT_VERSION branch when it replays an original run.
        WorkflowReplayer.replayWorkflowExecution(original, FileProcessingWorkflowPatched.class);
        // With the old branch removed, replaying an original run fails, as the comment in the code says.
        assertThrows(Exception.class,
            () -> WorkflowReplayer.replayWorkflowExecution(original, FileProcessingWorkflowPatchRemoved.class));
    }

    @Test
    public void newRunsTakeTheNewBranch() throws Exception {
        worker.registerWorkflowImplementationTypes(FileProcessingWorkflowPatched.class);
        env.start();

        WorkflowExecutionHistory patched = runFileProcessing("patched");
        assertTrue(activityTypes(patched).contains("CalculateChecksum"));
        assertTrue(activityTypes(patched).contains("UploadWithChecksum"));
        assertTrue(patched.getEvents().stream()
            .anyMatch(e -> e.getEventType() == EventType.EVENT_TYPE_MARKER_RECORDED));

        WorkflowReplayer.replayWorkflowExecution(patched, FileProcessingWorkflowPatchRemoved.class);
    }

    @Test
    public void autoUpsertRecordsChangeVersion() {
        ChangeVersionExample.registerWithAutoUpsert(worker);
        env.start();

        stub(ChangeVersionExample.YourWorkflow.class, "auto-upsert").run();
        assertTrue(changeVersionUpserts(client.fetchHistory("auto-upsert")).contains("checksumAdded-1"));
    }

    @Test
    public void manualUpsertRecordsChangeVersion() {
        worker.registerWorkflowImplementationTypes(
            ChangeVersionExample.ManualUpsertWorkflowImpl.class,
            ChangeVersionExample.MultipleVersionsWorkflowImpl.class);
        env.start();

        stub(ChangeVersionExample.ManualUpsertWorkflow.class, "manual-upsert").run();
        assertTrue(changeVersionUpserts(client.fetchHistory("manual-upsert")).contains("MovedThankYouAfterLoop-1"));

        stub(ChangeVersionExample.MultipleVersionsWorkflow.class, "manual-upsert-multiple").run();
        String upserts = changeVersionUpserts(client.fetchHistory("manual-upsert-multiple"));
        assertTrue(upserts.contains("versionOne-1"));
        assertTrue(upserts.contains("versionTwo-1"));
    }

    @Test
    public void cutoverRegistersBothWorkflowTypes() {
        PizzaCutoverExample.registerBoth(worker);
        env.start();

        PizzaCutoverExample.PizzaOrder order = new PizzaCutoverExample.PizzaOrder("order-1");
        assertEquals("SUCCESS",
            stub(PizzaCutoverExample.PizzaWorkflow.class, "pizza").pizzaWorkflow(order).status);
        assertEquals("SUCCESS",
            stub(PizzaCutoverExample.PizzaWorkflowV2.class, "pizza-v2").pizzaWorkflow(order).status);
    }
}
