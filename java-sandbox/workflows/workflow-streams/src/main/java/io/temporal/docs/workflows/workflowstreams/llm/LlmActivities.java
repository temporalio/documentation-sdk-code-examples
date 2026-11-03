package io.temporal.docs.workflows.workflowstreams.llm;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface LlmActivities {
  String streamCompletion(String prompt);
}
