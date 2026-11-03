package io.temporal.docs.workflows.workflowstreams.orders;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface DeltaActivities {
  void streamDeltas(String orderId);
}
