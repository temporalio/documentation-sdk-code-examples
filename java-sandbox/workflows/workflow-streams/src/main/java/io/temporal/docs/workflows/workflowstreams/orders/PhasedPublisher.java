package io.temporal.docs.workflows.workflowstreams.orders;

import io.temporal.client.WorkflowClient;
import io.temporal.workflowstreams.TopicHandle;
import io.temporal.workflowstreams.WorkflowStreamClient;
import java.util.ArrayList;
import java.util.List;

public class PhasedPublisher {
  final List<String> checkpoints = new ArrayList<>();

  public void publishUrgent(WorkflowClient workflowClient, String workflowId) {
    try (WorkflowStreamClient streamClient =
        WorkflowStreamClient.newInstance(workflowClient, workflowId)) {
      TopicHandle deltas = streamClient.topic("delta");
      Delta delta = new Delta("first");
      // @@@SNIPSTART java-workflow-streams-force-flush
      deltas.publish(delta, /* forceFlush */ true);
      // @@@SNIPEND
    }
  }

  public void publishInPhases(WorkflowClient workflowClient, String workflowId) {
    // @@@SNIPSTART java-workflow-streams-flush-barrier
    try (WorkflowStreamClient streamClient =
        WorkflowStreamClient.newInstance(workflowClient, workflowId)) {
      TopicHandle deltas = streamClient.topic("delta");

      for (Delta delta : firstPhase()) {
        deltas.publish(delta);
      }

      streamClient.flush();
      String checkpointId = recordPhaseOneComplete(); // only safe once phase-one events are durable

      for (Delta delta : secondPhase(checkpointId)) {
        deltas.publish(delta);
      }
    }
    // @@@SNIPEND
  }

  private List<Delta> firstPhase() {
    return List.of(new Delta("phase-1a"), new Delta("phase-1b"));
  }

  private String recordPhaseOneComplete() {
    String checkpointId = "checkpoint-" + checkpoints.size();
    checkpoints.add(checkpointId);
    return checkpointId;
  }

  private List<Delta> secondPhase(String checkpointId) {
    return List.of(new Delta("phase-2 after " + checkpointId));
  }
}
