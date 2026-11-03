package io.temporal.docs.activities.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.ActivityTaskScheduledEventAttributes;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.docs.activities.execution.ActivityOptionsExample.GreetingWorkflow;
import io.temporal.docs.activities.execution.FileProcessing.Arguments;
import io.temporal.docs.activities.execution.FileProcessing.FileProcessingActivities;
import io.temporal.docs.activities.execution.FileProcessing.FileProcessingWorkflow;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ActivityExecutionTest {

    private static final String TASK_QUEUE = "activity-execution";

    private TestWorkflowEnvironment env;
    private Worker worker;
    private WorkflowClient client;

    static class FakeFileActivities implements FileProcessingActivities {
        final List<String> calls = Collections.synchronizedList(new ArrayList<>());

        @Override
        public String download(String bucketName, String filename) {
            calls.add("download " + filename);
            return "/tmp/" + filename;
        }

        @Override
        public String processFile(String localName) {
            calls.add("processFile " + localName);
            return localName + ".processed";
        }

        @Override
        public List<String> processFiles(List<String> localNames) {
            calls.add("processFiles " + localNames.size());
            return localNames.stream().map(n -> n + ".processed").collect(Collectors.toList());
        }

        @Override
        public void upload(String bucketName, String filename, String localName) {
            calls.add("upload " + localName);
        }

        @Override
        public void deleteLocalFile(String localName) {
            calls.add("delete " + localName);
        }
    }

    static class GreetingActivitiesImpl implements GreetingActivities {
        @Override
        public String composeGreeting(String greeting, String name) {
            return greeting + " " + name + "!";
        }

        @Override
        public String getCustomerGreeting(String name) {
            return "Hi " + name;
        }

        @Override
        public String emailCustomerGreeting(String name) {
            return "Emailed " + name;
        }
    }

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
            type, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build());
    }

    private ActivityTaskScheduledEventAttributes scheduled(String workflowId) {
        return client.fetchHistory(workflowId).getEvents().stream()
            .filter(e -> e.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED)
            .findFirst()
            .orElseThrow()
            .getActivityTaskScheduledEventAttributes();
    }

    private static final Arguments ARGS =
        new Arguments("source", List.of("a.txt", "b.txt"), "target", "out.txt");

    @Test
    public void syncInvocation() {
        FakeFileActivities activities = new FakeFileActivities();
        worker.registerWorkflowImplementationTypes(SyncFileProcessingWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        env.start();

        stub(FileProcessingWorkflow.class, "sync").processFile(ARGS);
        assertEquals(List.of("download a.txt", "processFile /tmp/a.txt", "upload /tmp/a.txt.processed",
            "delete /tmp/a.txt", "delete /tmp/a.txt.processed"), activities.calls);
        assertEquals(Duration.ofHours(1).getSeconds(), scheduled("sync").getStartToCloseTimeout().getSeconds());
    }

    @Test
    public void multipleStubsUseTheirOwnTaskQueues() {
        FakeFileActivities activities = new FakeFileActivities();
        worker.registerWorkflowImplementationTypes(MultipleStubsWorkflowImpl.class);
        env.newWorker("taskQueue1").registerActivitiesImplementations(activities);
        env.newWorker("taskQueue2").registerActivitiesImplementations(activities);
        env.start();

        stub(FileProcessingWorkflow.class, "multiple-stubs").processFile(ARGS);
        List<String> queues = client.fetchHistory("multiple-stubs").getEvents().stream()
            .filter(e -> e.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED)
            .map(e -> e.getActivityTaskScheduledEventAttributes().getTaskQueue().getName())
            .collect(Collectors.toList());
        assertEquals(List.of("taskQueue1", "taskQueue2"), queues);
    }

    @Test
    public void untypedStub() {
        worker.registerWorkflowImplementationTypes(UntypedStubExample.UntypedGreetingWorkflowImpl.class);
        env.newWorker("simple-queue-node").registerActivitiesImplementations(new GreetingActivitiesImpl());
        env.start();

        assertEquals("Hello World Spanish!",
            stub(UntypedStubExample.UntypedGreetingWorkflow.class, "untyped").greet());
    }

    @Test
    public void asyncInvocationDownloadsAndUploadsInParallel() {
        FakeFileActivities activities = new FakeFileActivities();
        worker.registerWorkflowImplementationTypes(AsyncFileProcessingWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        env.start();

        stub(FileProcessingWorkflow.class, "async").processFile(ARGS);
        assertTrue(activities.calls.containsAll(List.of("download a.txt", "download b.txt", "processFiles 2",
            "upload /tmp/a.txt.processed", "upload /tmp/b.txt.processed")), activities.calls.toString());
    }

    @WorkflowInterface
    public interface DownloadOneWorkflow {
        @WorkflowMethod
        String run();
    }

    public static class DownloadOneWorkflowImpl implements DownloadOneWorkflow {
        @Override
        public String run() {
            return AsyncFileProcessingWorkflowImpl.downloadOne(
                Workflow.newActivityStub(FileProcessingActivities.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build()),
                "bucket", "file.txt");
        }
    }

    @Test
    public void asyncFunctionReturnsPromise() {
        worker.registerWorkflowImplementationTypes(DownloadOneWorkflowImpl.class);
        worker.registerActivitiesImplementations(new FakeFileActivities());
        env.start();

        assertEquals("/tmp/file.txt", stub(DownloadOneWorkflow.class, "download-one").run());
    }

    private ActivityTaskScheduledEventAttributes runStub(Class<?> impl, String option) {
        worker.registerWorkflowImplementationTypes(impl);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        env.newWorker("yourTaskQueue").registerActivitiesImplementations(new GreetingActivitiesImpl());
        env.start();
        String workflowId = "stub-" + option;
        stub(GreetingWorkflow.class, workflowId).greet(option);
        return scheduled(workflowId);
    }

    @Test
    public void stubTimeouts() {
        assertEquals(5, runStub(ActivityOptionsExample.StubOptionsWorkflowImpl.class, "timeouts")
            .getScheduleToCloseTimeout().getSeconds());
    }

    @Test
    public void stubScheduleToStart() {
        ActivityTaskScheduledEventAttributes a =
            runStub(ActivityOptionsExample.StubOptionsWorkflowImpl.class, "scheduleToStart");
        assertEquals(5, a.getScheduleToStartTimeout().getSeconds());
        assertEquals(20, a.getScheduleToCloseTimeout().getSeconds());
    }

    @Test
    public void stubStartToClose() {
        assertEquals(2, runStub(ActivityOptionsExample.StubOptionsWorkflowImpl.class, "startToClose")
            .getStartToCloseTimeout().getSeconds());
    }

    @Test
    public void stubTaskQueue() {
        assertEquals("yourTaskQueue", runStub(ActivityOptionsExample.StubOptionsWorkflowImpl.class, "taskQueue")
            .getTaskQueue().getName());
    }

    @Test
    public void stubHeartbeat() {
        assertEquals(2, runStub(ActivityOptionsExample.HeartbeatWorkflowImpl.class, "heartbeat")
            .getHeartbeatTimeout().getSeconds());
    }

    @Test
    public void stubRetryOptions() {
        assertEquals(10, runStub(ActivityOptionsExample.RetryOptionsWorkflowImpl.class, "retry")
            .getRetryPolicy().getMaximumInterval().getSeconds());
    }

    @Test
    public void stubCancellationType() {
        assertEquals(5, runStub(ActivityOptionsExample.CancellationTypeWorkflowImpl.class, "cancellation")
            .getStartToCloseTimeout().getSeconds());
    }

    private ActivityTaskScheduledEventAttributes runWithOptions(
        WorkflowImplementationOptions options, String option) {
        worker.registerWorkflowImplementationTypes(
            options, ActivityOptionsExample.WorkflowImplementationOptionsWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        env.newWorker("yourTaskQueue").registerActivitiesImplementations(new GreetingActivitiesImpl());
        env.start();
        String workflowId = "wio-" + option;
        stub(GreetingWorkflow.class, workflowId).greet(option);
        return scheduled(workflowId);
    }

    @Test
    public void workflowImplementationOptionsApplyPerActivityType() {
        ActivityTaskScheduledEventAttributes a = runWithOptions(ActivityOptionsExample.timeouts(), "getTimeouts");
        assertEquals("GetCustomerGreeting", a.getActivityType().getName());
        assertEquals(5, a.getScheduleToCloseTimeout().getSeconds());
    }

    @Test
    public void wioScheduleToClose() {
        assertEquals(5, runWithOptions(ActivityOptionsExample.scheduleToClose(), "getScheduleToClose")
            .getScheduleToCloseTimeout().getSeconds());
    }

    @Test
    public void wioScheduleToStart() {
        assertEquals(5, runWithOptions(ActivityOptionsExample.scheduleToStart(), "getScheduleToStart")
            .getScheduleToStartTimeout().getSeconds());
    }

    @Test
    public void wioStartToClose() {
        assertEquals(2, runWithOptions(ActivityOptionsExample.startToClose(), "emailStartToClose")
            .getStartToCloseTimeout().getSeconds());
    }

    @Test
    public void wioHeartbeat() {
        assertEquals(2, runWithOptions(ActivityOptionsExample.heartbeat(), "emailHeartbeat")
            .getHeartbeatTimeout().getSeconds());
    }

    @Test
    public void wioTaskQueue() {
        assertEquals("yourTaskQueue", runWithOptions(ActivityOptionsExample.taskQueue(), "emailTaskQueue")
            .getTaskQueue().getName());
    }

    @Test
    public void wioRetryOptions() {
        assertEquals(List.of(NullPointerException.class.getName()),
            runWithOptions(ActivityOptionsExample.retry(), "emailRetry").getRetryPolicy().getNonRetryableErrorTypesList());
    }

    @Test
    public void wioCancellationType() {
        assertEquals(5, runWithOptions(ActivityOptionsExample.cancellation(), "emailCancellation")
            .getStartToCloseTimeout().getSeconds());
    }

    @WorkflowInterface
    public interface DownloadWorkflow {
        @WorkflowMethod
        String download(String remoteName);
    }

    public static class ContextDownloadWorkflowImpl implements DownloadWorkflow {
        @Override
        public String download(String remoteName) {
            return Workflow.newActivityStub(
                    io.temporal.docs.activities.execution.context.FileProcessingActivities.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build())
                .download("bucket", remoteName, "local.txt");
        }
    }

    public static class AsyncCompletionDownloadWorkflowImpl implements DownloadWorkflow {
        @Override
        public String download(String remoteName) {
            return Workflow.newActivityStub(
                    io.temporal.docs.activities.execution.asynccompletion.FileProcessingActivities.class,
                    ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(10))
                        .setRetryOptions(io.temporal.common.RetryOptions.newBuilder().setMaximumAttempts(1).build())
                        .build())
                .download("bucket", remoteName, "local.txt");
        }
    }

    @Test
    public void activityExecutionContext() {
        worker.registerWorkflowImplementationTypes(ContextDownloadWorkflowImpl.class);
        worker.registerActivitiesImplementations(
            new io.temporal.docs.activities.execution.context.FileProcessingActivitiesImpl());
        env.start();

        assertEquals("/tmp/local.txt", stub(DownloadWorkflow.class, "context").download("remote.txt"));
    }

    @Test
    public void completeActivityFromAnotherThread() {
        io.temporal.docs.activities.execution.asynccompletion.DownloadService service =
            new io.temporal.docs.activities.execution.asynccompletion.DownloadService(
                client.newActivityCompletionClient());
        worker.registerWorkflowImplementationTypes(AsyncCompletionDownloadWorkflowImpl.class);
        worker.registerActivitiesImplementations(
            new io.temporal.docs.activities.execution.asynccompletion.FileProcessingActivitiesImpl(service));
        env.start();

        // The Activity's return value is ignored; the service's result completes it.
        assertEquals("/tmp/local.txt", stub(DownloadWorkflow.class, "async-complete").download("remote.txt"));
        assertThrows(WorkflowFailedException.class,
            () -> stub(DownloadWorkflow.class, "async-fail").download("missing.txt"));
    }
}
