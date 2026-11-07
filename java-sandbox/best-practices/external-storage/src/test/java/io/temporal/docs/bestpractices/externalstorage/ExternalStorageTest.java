package io.temporal.docs.bestpractices.externalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.api.common.v1.Payload;
import io.temporal.api.enums.v1.EventType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.payload.storage.ExternalStorage;
import io.temporal.payload.storage.s3driver.S3StorageDriver;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ExternalStorageTest {

    private static final String TASK_QUEUE = "external-storage";
    private static final String SMALL = "small value";
    // Larger than the 256 KiB default threshold once serialized.
    private static final String LARGE = "x".repeat(300 * 1024);

    private static TestWorkflowEnvironment env(ExternalStorage storage) {
        TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance(
            TestEnvironmentOptions.newBuilder()
                .setWorkflowClientOptions(
                    WorkflowClientOptions.newBuilder().setExternalStorage(storage).build())
                .build());
        Worker worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(EchoWorkflow.Impl.class);
        worker.registerActivitiesImplementations(new EchoWorkflow.EchoActivitiesImpl());
        env.start();
        return env;
    }

    private static String run(WorkflowClient client, String workflowId, String value) {
        return client.newWorkflowStub(EchoWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(TASK_QUEUE).build())
            .echo(value);
    }

    private static Payload startInput(WorkflowClient client, String workflowId) {
        return client.fetchHistory(workflowId).getEvents().stream()
            .filter(e -> e.getEventType() == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_STARTED)
            .findFirst().orElseThrow()
            .getWorkflowExecutionStartedEventAttributes().getInput().getPayloads(0);
    }

    private static long fileCount(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    @Test
    public void zeroThresholdOffloadsEveryPayloadToLocalDisk(@TempDir Path storeDir) throws IOException {
        ExternalStorage storage = ExternalStorageConfigurationExamples.configureThreshold(
            new LocalDiskStorageDriver(storeDir));
        TestWorkflowEnvironment env = env(storage);
        try {
            WorkflowClient client = env.getWorkflowClient();
            assertEquals(SMALL, run(client, "small", SMALL));

            // Even the small input is replaced by a reference in Event History.
            assertFalse(startInput(client, "small").getData().toStringUtf8().contains(SMALL));
            // Files are grouped by Namespace and Workflow ID. Workflow input and result, and
            // Activity input and result.
            assertEquals(4, fileCount(storeDir.resolve("default").resolve("small")));
        } finally {
            env.close();
        }
    }

    @Test
    public void defaultThresholdKeepsSmallPayloadsInline(@TempDir Path storeDir) throws IOException {
        TestWorkflowEnvironment env = env(
            ExternalStorage.newBuilder().setDriver(new LocalDiskStorageDriver(storeDir)).build());
        try {
            WorkflowClient client = env.getWorkflowClient();
            assertEquals(SMALL, run(client, "small", SMALL));
            assertTrue(startInput(client, "small").getData().toStringUtf8().contains(SMALL));
            assertEquals(0, fileCount(storeDir));

            assertEquals(LARGE, run(client, "large", LARGE));
            assertTrue(startInput(client, "large").getSerializedSize() < 1024);
            assertEquals(4, fileCount(storeDir.resolve("default").resolve("large")));
        } finally {
            env.close();
        }
    }

    private static S3StorageDriver s3Driver(InMemoryS3Client s3, String name) {
        return S3StorageDriver.newBuilder()
            .setClient(s3)
            .setBucket("my-temporal-payloads")
            .setName(name)
            .build();
    }

    @Test
    public void s3DriverReusesObjectsAndVerifiesHashes() {
        InMemoryS3Client s3 = new InMemoryS3Client();
        TestWorkflowEnvironment env = env(
            ExternalStorage.newBuilder().setDriver(s3Driver(s3, "s3")).build());
        try {
            WorkflowClient client = env.getWorkflowClient();
            assertEquals(LARGE, run(client, "dedup", LARGE));

            // The same value is stored four times. The Client stores the Workflow input before
            // the Run ID exists, so its key has no Run ID. The Worker stores the Activity input,
            // the Activity result, and the Workflow result under the Run's key, and uploads only
            // the first: the driver reuses the existing object for the other two.
            assertEquals(2, s3.puts.get());
            assertEquals(2, s3.objects.size());
            assertEquals(1, s3.objects.keySet().stream().filter(k -> k.contains("/ri/null/")).count());

            // Change the stored object: retrieval fails the hash check.
            s3.objects.replaceAll((key, data) -> {
                byte[] changed = data.clone();
                changed[changed.length - 1] ^= 1;
                return changed;
            });
            WorkflowStub stub = client.newUntypedWorkflowStub("dedup");
            assertThrows(Exception.class, () -> stub.getResult(String.class));
        } finally {
            env.close();
        }
    }

    @Test
    public void selectorStoresWithPreferredDriverAndLegacyStillRetrieves() {
        InMemoryS3Client preferredS3 = new InMemoryS3Client();
        InMemoryS3Client legacyS3 = new InMemoryS3Client();
        S3StorageDriver preferred = s3Driver(preferredS3, "preferred");
        S3StorageDriver legacy = s3Driver(legacyS3, "legacy");

        // The Worker uses the page's two-driver configuration.
        TestWorkflowEnvironment env = env(
            ExternalStorageConfigurationExamples.configureMultipleDrivers(preferred, legacy));
        try {
            // A Client that still has only the legacy driver starts the Workflow.
            WorkflowClient legacyClient = WorkflowClient.newInstance(
                env.getWorkflowServiceStubs(),
                WorkflowClientOptions.newBuilder()
                    .setExternalStorage(ExternalStorage.newBuilder().setDriver(legacy).build())
                    .build());
            WorkflowStub stub = WorkflowStub.fromTyped(legacyClient.newWorkflowStub(EchoWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId("migrate").setTaskQueue(TASK_QUEUE).build()));
            stub.start(LARGE);

            // The Worker reads the input with the legacy driver and stores new Payloads with the
            // preferred one.
            assertEquals(LARGE, env.getWorkflowClient().newUntypedWorkflowStub("migrate")
                .getResult(String.class));
            assertEquals(1, legacyS3.puts.get());
            assertEquals(1, preferredS3.puts.get());
        } finally {
            env.close();
        }
    }
}
