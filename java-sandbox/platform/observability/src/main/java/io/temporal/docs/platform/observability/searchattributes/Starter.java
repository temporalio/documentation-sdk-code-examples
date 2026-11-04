package io.temporal.docs.platform.observability.searchattributes;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.SearchAttributes;
import io.temporal.serviceclient.WorkflowServiceStubs;

// @@@SNIPSTART java-observability-set-search-attributes
public class Starter {

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        String workflowID = "pizza-order-1";

        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setWorkflowId(workflowID)
            .setTaskQueue(Constants.TASK_QUEUE_NAME)
            .setTypedSearchAttributes(generateSearchAttributes())
            .build();

        PizzaWorkflow workflow = client.newWorkflowStub(PizzaWorkflow.class, options);
        WorkflowClient.start(workflow::orderPizza, "701 Mission Street");
        System.exit(0);
    }

    public static SearchAttributes generateSearchAttributes() {
        return SearchAttributes.newBuilder().set(Constants.IS_ORDER_FAILED, false).build();
    }
}
// @@@SNIPEND
