package io.temporal.docs.workers.cloudrun.heartbeat;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import java.util.List;

// @@@SNIPSTART java-cloud-run-heartbeat
public class ItemActivitiesImpl implements ItemActivities {
  @Override
  public String process(List<String> items) {
    ActivityExecutionContext context = Activity.getExecutionContext();
    // On a retry, start after the last item the previous attempt recorded.
    int start = context.getHeartbeatDetails(Integer.class).map(i -> i + 1).orElse(0);
    for (int i = start; i < items.size(); i++) {
      processItem(items.get(i));
      context.heartbeat(i);
    }
    return "done";
  }

  protected void processItem(String item) {
    // ... process the item
  }
}
// @@@SNIPEND
