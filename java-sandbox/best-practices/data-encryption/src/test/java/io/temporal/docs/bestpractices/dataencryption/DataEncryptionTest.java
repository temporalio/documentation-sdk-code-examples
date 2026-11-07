package io.temporal.docs.bestpractices.dataencryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.payload.codec.PayloadCodecException;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

public class DataEncryptionTest {

    private static final String TASK_QUEUE = "encrypted-payloads";
    private static final String SECRET = "My Secret Friend";

    private static SecretKey newKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return new SecretKeySpec(bytes, "AES");
    }

    private static final Map<String, SecretKey> KEYS = Map.of("key-1", newKey());

    private static String encoding(Payload payload) {
        return payload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8();
    }

    private static HistoryEvent firstEvent(WorkflowClient client, String workflowId, EventType type) {
        return client.fetchHistory(workflowId).getEvents().stream()
            .filter(e -> e.getEventType() == type)
            .findFirst()
            .orElseThrow();
    }

    private static TestWorkflowEnvironment env(WorkflowClientOptions options) {
        TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance(
            TestEnvironmentOptions.newBuilder().setWorkflowClientOptions(options).build());
        Worker worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        env.start();
        return env;
    }

    private static GreetingWorkflow stub(WorkflowClient client, String workflowId) {
        return client.newWorkflowStub(GreetingWorkflow.class,
            WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(TASK_QUEUE).build());
    }

    @Test
    public void historyHoldsOnlyEncryptedPayloads() {
        TestWorkflowEnvironment env = env(Clients.clientOptions(KEYS, "key-1"));
        try {
            WorkflowClient client = env.getWorkflowClient();
            assertEquals("Hello " + SECRET + "!", stub(client, "ok").getGreeting(SECRET));

            Payload input = firstEvent(client, "ok", EventType.EVENT_TYPE_WORKFLOW_EXECUTION_STARTED)
                .getWorkflowExecutionStartedEventAttributes().getInput().getPayloads(0);
            Payload result = firstEvent(client, "ok", EventType.EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED)
                .getWorkflowExecutionCompletedEventAttributes().getResult().getPayloads(0);
            for (Payload payload : List.of(input, result)) {
                assertEquals("binary/encrypted", encoding(payload));
                assertEquals("key-1",
                    payload.getMetadataOrThrow("encryption-key-id").toStringUtf8());
                assertFalse(payload.getData().toStringUtf8().contains(SECRET));
            }
        } finally {
            env.close();
        }
    }

    @Test
    public void failureMessagesAreEncryptedOnlyWhenEnabled() {
        // Enabled, as in the page's Client options.
        TestWorkflowEnvironment env = env(Clients.clientOptions(KEYS, "key-1"));
        try {
            WorkflowClient client = env.getWorkflowClient();
            WorkflowFailedException e =
                assertThrows(WorkflowFailedException.class, () -> stub(client, "bad").getGreeting(""));
            // The Client decrypts the failure, so the message is readable here...
            assertTrue(e.getCause().getCause().getMessage().contains("Name is empty"));
            // ...but Event History holds only a placeholder message.
            String stored = firstEvent(client, "bad", EventType.EVENT_TYPE_ACTIVITY_TASK_FAILED)
                .getActivityTaskFailedEventAttributes().getFailure().getMessage();
            assertFalse(stored.contains("Name is empty"), stored);
        } finally {
            env.close();
        }

        // Disabled, the default for the two-argument constructor.
        WorkflowClientOptions plain = WorkflowClientOptions.newBuilder()
            .setDataConverter(new CodecDataConverter(
                DefaultDataConverter.newDefaultInstance(),
                List.of(new EncryptionCodec(KEYS, "key-1"))))
            .build();
        env = env(plain);
        try {
            WorkflowClient client = env.getWorkflowClient();
            assertThrows(WorkflowFailedException.class, () -> stub(client, "bad").getGreeting(""));
            String stored = firstEvent(client, "bad", EventType.EVENT_TYPE_ACTIVITY_TASK_FAILED)
                .getActivityTaskFailedEventAttributes().getFailure().getMessage();
            assertTrue(stored.contains("Name is empty"), stored);
        } finally {
            env.close();
        }
    }

    private static DataConverter converter(Map<String, SecretKey> keys, String activeKeyId) {
        return Clients.clientOptions(keys, activeKeyId).getDataConverter();
    }

    @Test
    public void rotatedCodecStillDecryptsOlderPayloads() {
        SecretKey key2 = newKey();
        Payload old = converter(KEYS, "key-1").toPayload(SECRET).orElseThrow();

        DataConverter rotated = converter(Map.of("key-1", KEYS.get("key-1"), "key-2", key2), "key-2");
        assertEquals(SECRET, rotated.fromPayload(old, String.class, String.class));
        assertEquals("key-2", rotated.toPayload(SECRET).orElseThrow()
            .getMetadataOrThrow("encryption-key-id").toStringUtf8());
    }

    @Test
    public void wrongOrMissingKeyAndTamperingFail() {
        Payload encrypted = converter(KEYS, "key-1").toPayload(SECRET).orElseThrow();
        EncryptionCodec otherKey = new EncryptionCodec(Map.of("key-1", newKey()), "key-1");
        EncryptionCodec missingKey = new EncryptionCodec(Map.of("key-2", newKey()), "key-2");
        assertThrows(PayloadCodecException.class, () -> otherKey.decode(List.of(encrypted)));
        assertThrows(PayloadCodecException.class, () -> missingKey.decode(List.of(encrypted)));

        byte[] data = encrypted.getData().toByteArray();
        data[data.length - 1] ^= 1;
        Payload tampered = encrypted.toBuilder().setData(ByteString.copyFrom(data)).build();
        EncryptionCodec codec = new EncryptionCodec(KEYS, "key-1");
        assertThrows(PayloadCodecException.class, () -> codec.decode(List.of(tampered)));
    }

    @Test
    public void unencryptedPayloadsPassThroughDecode() {
        Payload plain = DefaultDataConverter.STANDARD_INSTANCE.toPayload(SECRET).orElseThrow();
        assertEquals(plain, new EncryptionCodec(KEYS, "key-1").decode(List.of(plain)).get(0));
    }
}
