package quickstart;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {
    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) throws Exception {
        // Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
        // selects the profile, the TOML file supplies address/namespace/credentials,
        // and TEMPORAL_* env vars can override individual fields.
        ClientConfigProfile profile = ClientConfigProfile.load();
        WorkflowServiceStubsOptions serviceOptions = profile.toWorkflowServiceStubsOptions();
        WorkflowClientOptions clientOptions = profile.toWorkflowClientOptions();

        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(serviceOptions);
        WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

        WorkflowOptions options =
                WorkflowOptions.newBuilder()
                        .setWorkflowId("batch-iterator-" + System.currentTimeMillis())
                        .setTaskQueue(BatchIterator.TASK_QUEUE)
                        .build();
        IteratorBatchWorkflow batch = client.newWorkflowStub(IteratorBatchWorkflow.class, options);

        WorkflowExecution execution =
                WorkflowClient.start(batch::processBatch, BatchIterator.PAGE_SIZE, 0);
        log.info(
                "Started batch Workflow {} against \"{}\" (namespace \"{}\")...",
                execution.getWorkflowId(),
                serviceOptions.getTarget(),
                clientOptions.getNamespace());

        // getResult follows the continue-as-new chain and returns the final run's result.
        int processed = WorkflowStub.fromTyped(batch).getResult(Integer.class);
        log.info("Batch complete: processed {} records", processed);

        service.shutdown();
    }
}
