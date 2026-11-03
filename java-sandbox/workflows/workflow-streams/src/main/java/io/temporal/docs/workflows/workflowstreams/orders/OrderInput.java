package io.temporal.docs.workflows.workflowstreams.orders;

import io.temporal.workflowstreams.WorkflowStreamState;

public class OrderInput {
  public String orderId;
  public WorkflowStreamState streamState;

  public OrderInput() {}

  public OrderInput(String orderId) {
    this.orderId = orderId;
  }
}
