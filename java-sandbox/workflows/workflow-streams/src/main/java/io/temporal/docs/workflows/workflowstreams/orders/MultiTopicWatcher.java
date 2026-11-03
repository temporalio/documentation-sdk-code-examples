package io.temporal.docs.workflows.workflowstreams.orders;

import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.workflowstreams.SubscribeOptions;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamSubscription;

public class MultiTopicWatcher {
  public void watch(WorkflowStreamClient stream) {
    // @@@SNIPSTART java-workflow-streams-heterogeneous-topics
    SubscribeOptions options =
        SubscribeOptions.newBuilder().setTopics("status", "progress").build();
    try (WorkflowStreamSubscription subscription = stream.subscribe(options)) {
      for (WorkflowStreamItem item : subscription) {
        switch (item.getTopic()) {
          case "status":
            StatusEvent status =
                DefaultDataConverter.STANDARD_INSTANCE.fromPayload(
                    item.getPayload(), StatusEvent.class, StatusEvent.class);
            System.out.printf("[status] %s: %s%n", status.state, status.detail);
            break;
          case "progress":
            ProgressEvent progress =
                DefaultDataConverter.STANDARD_INSTANCE.fromPayload(
                    item.getPayload(), ProgressEvent.class, ProgressEvent.class);
            System.out.printf("[progress] %s%n", progress.message);
            break;
        }
      }
    }
    // @@@SNIPEND
  }
}
