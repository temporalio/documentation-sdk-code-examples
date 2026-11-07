package io.temporal.docs.bestpractices.dataencryption;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;

public class Clients {

    public static WorkflowClientOptions clientOptions(Map<String, SecretKey> keys, String activeKeyId) {
        // @@@SNIPSTART java-data-encryption-set-codec
        WorkflowClientOptions options =
            WorkflowClientOptions.newBuilder()
                .setDataConverter(
                    new CodecDataConverter(
                        DefaultDataConverter.newDefaultInstance(),
                        List.of(new EncryptionCodec(keys, activeKeyId)),
                        // Also encrypt Failure messages and stack traces.
                        true))
                .build();
        // @@@SNIPEND
        return options;
    }

    // Use the same Client options in your Worker and in every Client that starts Workflows.
    public static WorkflowClient createClient(
            WorkflowServiceStubs service, Map<String, SecretKey> keys, String activeKeyId) {
        return WorkflowClient.newInstance(service, clientOptions(keys, activeKeyId));
    }
}
