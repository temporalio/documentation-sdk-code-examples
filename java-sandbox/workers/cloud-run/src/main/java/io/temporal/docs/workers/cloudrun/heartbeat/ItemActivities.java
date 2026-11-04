package io.temporal.docs.workers.cloudrun.heartbeat;

import io.temporal.activity.ActivityInterface;
import java.util.List;

@ActivityInterface
public interface ItemActivities {
  String process(List<String> items);
}
