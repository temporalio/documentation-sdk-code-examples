package io.temporal.docs.workflows.workflowstreams.orders;

// @@@SNIPSTART java-workflow-streams-subscribe-iterator
import io.temporal.client.WorkflowClient;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.workflowstreams.SubscribeOptions;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamSubscription;

public class OrderWatcher {
  public void watchOrder(WorkflowClient workflowClient, String orderId) {
    SubscribeOptions options = SubscribeOptions.newBuilder().setTopics("status").build();
    try (WorkflowStreamClient stream = WorkflowStreamClient.newInstance(workflowClient, orderId);
        WorkflowStreamSubscription subscription = stream.subscribe(options)) {
      for (WorkflowStreamItem item : subscription) {
        StatusEvent evt =
            DefaultDataConverter.STANDARD_INSTANCE.fromPayload(
                item.getPayload(), StatusEvent.class, StatusEvent.class);
        System.out.printf("[%3d%%] %s: %s%n", evt.progress, evt.state, evt.detail);
        if (evt.state.equals("completed")) {
          break;
        }
      }
    }
  }
}
// @@@SNIPEND
