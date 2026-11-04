package io.temporal.docs.platform.enrichingui;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;

public class AsyncStart {
    public static WorkflowExecution startWorkflow(WorkflowClient workflowClient) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setWorkflowId("your-async-workflow-id")
            .setTaskQueue("your-task-queue")
            .setStaticSummary("Order processing for customer #12345")
            .setStaticDetails("Processing premium order with expedited shipping")
            .build();
        YourWorkflow workflow = workflowClient.newWorkflowStub(YourWorkflow.class, options);

        // @@@SNIPSTART java-enriching-ui-async-start
        // Start workflow asynchronously
        WorkflowExecution execution = WorkflowClient.start(workflow::yourWorkflowMethod, "workflow input");
        // @@@SNIPEND
        return execution;
    }
}
