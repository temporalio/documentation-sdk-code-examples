package io.temporal.docs.workflows.workflowstreams.longrunning;

import io.temporal.workflowstreams.WorkflowStreamClientOptions;
import java.time.Duration;

public class RetryDurationOptions {
  public static WorkflowStreamClientOptions options() {
    // @@@SNIPSTART java-workflow-streams-max-retry-duration
    WorkflowStreamClientOptions options =
        WorkflowStreamClientOptions.newBuilder()
            .setMaxRetryDuration(Duration.ofMinutes(10))
            .build();
    // @@@SNIPEND
    return options;
  }
}
