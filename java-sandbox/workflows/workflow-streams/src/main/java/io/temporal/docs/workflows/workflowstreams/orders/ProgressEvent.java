package io.temporal.docs.workflows.workflowstreams.orders;

class ProgressEvent {
  public String message;

  public ProgressEvent() {}

  public ProgressEvent(String message) {
    this.message = message;
  }
}
