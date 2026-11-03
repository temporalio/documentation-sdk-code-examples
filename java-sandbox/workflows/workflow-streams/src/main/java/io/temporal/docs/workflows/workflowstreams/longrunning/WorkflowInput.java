package io.temporal.docs.workflows.workflowstreams.longrunning;

// @@@SNIPSTART java-workflow-streams-continue-as-new-input
import io.temporal.workflowstreams.WorkflowStreamState;

public class WorkflowInput {
  public int itemsProcessed;
  public WorkflowStreamState streamState;
}
// @@@SNIPEND
