package io.temporal.docs.workflows.workflowstreams.orders;

// @@@SNIPSTART java-workflow-streams-publish-from-client
import io.temporal.client.WorkflowClient;
import io.temporal.workflowstreams.TopicHandle;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamClientOptions;
import java.time.Duration;

public class StatusPublisher {
  public void publishStatus(WorkflowClient workflowClient, String workflowId) {
    WorkflowStreamClientOptions options =
        WorkflowStreamClientOptions.newBuilder()
            .setBatchInterval(Duration.ofMillis(200))
            .build();
    try (WorkflowStreamClient streamClient =
        WorkflowStreamClient.newInstance(workflowClient, workflowId, options)) {
      TopicHandle status = streamClient.topic("status");
      status.publish(new StatusEvent("started", 0, ""));
      // ...
      // Buffer is flushed automatically on close().
    }
  }
}
// @@@SNIPEND
