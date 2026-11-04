package io.temporal.docs.workers.cloudrun.perworkflow;

// @@@SNIPSTART java-cloud-run-workflow-versioning-behavior
import io.temporal.activity.ActivityOptions;
import io.temporal.common.VersioningBehavior;
import io.temporal.docs.workers.cloudrun.GreetingActivities;
import io.temporal.docs.workers.cloudrun.GreetingWorkflow;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowVersioningBehavior;
import java.time.Duration;

public class GreetingWorkflowImpl implements GreetingWorkflow {
  private final GreetingActivities activities =
      Workflow.newActivityStub(
          GreetingActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  @WorkflowVersioningBehavior(VersioningBehavior.PINNED)
  public String getGreeting(String name) {
    return activities.composeGreeting(name);
  }
}
// @@@SNIPEND
