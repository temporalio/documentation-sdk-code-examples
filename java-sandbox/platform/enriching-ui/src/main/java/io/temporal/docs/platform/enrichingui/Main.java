// @@@SNIPSTART java-enriching-ui-static-summary
package io.temporal.docs.platform.enrichingui;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class Main {
    public static void main(String[] args) {
        // Create service stubs and workflow client
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient workflowClient = WorkflowClient.newInstance(service);

        String result = runWorkflow(workflowClient);
        System.out.println(result);
        System.exit(0);
    }

    public static String runWorkflow(WorkflowClient workflowClient) {
        // Create workflow options with static summary and details
        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setWorkflowId("your-workflow-id")
            .setTaskQueue("your-task-queue")
            .setStaticSummary("Order processing for customer #12345")
            .setStaticDetails("Processing premium order with expedited shipping")
            .build();

        // Create the workflow stub
        YourWorkflow workflow = workflowClient.newWorkflowStub(YourWorkflow.class, options);

        // Start the workflow and wait for its result
        return workflow.yourWorkflowMethod("workflow input");
    }
}
// @@@SNIPEND
