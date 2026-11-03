package io.temporal.docs.workflows.workflowstreams.orders;

// @@@SNIPSTART java-workflow-streams-publish-from-activity
import io.temporal.activity.Activity;
import io.temporal.workflowstreams.TopicHandle;
import io.temporal.workflowstreams.WorkflowStreamClient;
import java.util.List;

class Delta {
  public String text;

  public Delta() {}

  public Delta(String text) {
    this.text = text;
  }
}

public class DeltaActivitiesImpl implements DeltaActivities {
  @Override
  public void streamDeltas(String orderId) {
    try (WorkflowStreamClient streamClient = WorkflowStreamClient.fromActivity()) {
      TopicHandle deltas = streamClient.topic("delta");
      for (Delta delta : generateDeltas(orderId)) {
        deltas.publish(delta);
        Activity.getExecutionContext().heartbeat(null);
      }
      // Buffer is flushed automatically on close().
    }
  }

  private List<Delta> generateDeltas(String orderId) {
    return List.of(new Delta("order "), new Delta(orderId), new Delta(" is ready"));
  }
}
// @@@SNIPEND
