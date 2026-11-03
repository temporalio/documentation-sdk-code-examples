package io.temporal.docs.workflows.workflowstreams.closing;

public class StatusEvent {
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
