package io.temporal.docs.activities.timeouts;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.docs.activities.timeouts.YourActivityDefinition.YourActivityMethodParam;
import java.util.ArrayList;
import java.util.List;

// @@@SNIPSTART java-activity-timeouts-heartbeat
public class YourActivityDefinitionImpl implements YourActivityDefinition {

  @Override
  public String yourActivityMethod(YourActivityMethodParam param) {
    ActivityExecutionContext context = Activity.getExecutionContext();
    // On a retry, resume after the last item the previous attempt reported.
    int start = context.getHeartbeatDetails(Integer.class).map(last -> last + 1).orElse(0);

    List<String> processed = new ArrayList<>();
    for (int i = start; i < param.items.size(); i++) {
      processed.add(processItem(param.items.get(i)));
      int details = i;
      // Report progress; the next attempt can read the details if this one fails.
      context.heartbeat(details);
    }
    return "attempt " + context.getInfo().getAttempt() + " processed " + processed;
  }

  protected String processItem(String item) {
    return item.toUpperCase();
  }
}
// @@@SNIPEND
