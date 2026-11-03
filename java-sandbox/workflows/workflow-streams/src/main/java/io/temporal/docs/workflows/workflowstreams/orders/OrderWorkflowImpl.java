package io.temporal.docs.workflows.workflowstreams.orders;

// @@@SNIPSTART java-workflow-streams-publish-from-workflow
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import io.temporal.workflowstreams.WorkflowTopicHandle;
import java.time.Duration;

class StatusEvent {
  public String state;
  public int progress;
  public String detail;

  public StatusEvent() {}

  public StatusEvent(String state, int progress, String detail) {
    this.state = state;
    this.progress = progress;
    this.detail = detail;
  }
}

public class OrderWorkflowImpl implements OrderWorkflow {
  private final WorkflowStream stream;
  private final WorkflowTopicHandle status;

  private final OrderActivities activities =
      Workflow.newActivityStub(
          OrderActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(1)).build());

  @WorkflowInit
  public OrderWorkflowImpl(OrderInput input) {
    stream = WorkflowStream.newInstance(input.streamState);
    status = stream.topic("status");
  }

  @Override
  public void execute(OrderInput input) {
    status.publish(new StatusEvent("validating", 0, "checking inventory"));
    activities.validateOrder(input.orderId);

    status.publish(new StatusEvent("charging", 33, "authorizing payment"));
    activities.chargePayment(input.orderId);

    status.publish(new StatusEvent("shipping", 66, "dispatching to warehouse"));
    activities.dispatchOrder(input.orderId);

    status.publish(new StatusEvent("completed", 100, ""));
  }
}
// @@@SNIPEND
