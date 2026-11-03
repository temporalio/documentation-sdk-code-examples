package io.temporal.docs.workflows.workflowstreams.llm;

// @@@SNIPSTART java-workflow-streams-llm-consumer
import io.temporal.client.WorkflowClient;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.workflowstreams.SubscribeOptions;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamSubscription;

public class Consumer {
  public String streamChat(WorkflowClient workflowClient, String chatId) {
    StringBuilder output = new StringBuilder();

    Runnable render =
        () -> {
          // ... display the accumulated output (terminal redraw, UI update, etc.)
        };

    SubscribeOptions options =
        SubscribeOptions.newBuilder().setTopics("delta", "retry", "close").build();
    try (WorkflowStreamClient stream = WorkflowStreamClient.newInstance(workflowClient, chatId);
        WorkflowStreamSubscription subscription = stream.subscribe(options)) {
      for (WorkflowStreamItem item : subscription) {
        switch (item.getTopic()) {
          case "retry":
            // Earlier attempt's deltas are stale; drop what we've shown.
            output.setLength(0);
            render.run();
            break;
          case "delta":
            TextDelta delta =
                DefaultDataConverter.STANDARD_INSTANCE.fromPayload(
                    item.getPayload(), TextDelta.class, TextDelta.class);
            output.append(delta.text);
            render.run();
            break;
          case "close":
            // Acknowledge so the Workflow can return without waiting on the fallback timeout.
            workflowClient
                .newUntypedWorkflowStub(chatId)
                .signal("subscriberAcknowledgedTerminator");
            return output.toString();
        }
      }
    }

    return output.toString();
  }
}
// @@@SNIPEND
