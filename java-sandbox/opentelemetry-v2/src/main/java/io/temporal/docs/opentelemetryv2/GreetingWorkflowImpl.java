package io.temporal.docs.opentelemetryv2;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

public class GreetingWorkflowImpl implements GreetingWorkflow {
  static final String INSTRUMENTATION_NAME = "io.temporal.docs.opentelemetryv2";

  // @@@SNIPSTART java-opentelemetry-v2-workflow-logger
  private static final Logger logger = Workflow.getLogger(GreetingWorkflowImpl.class);
  // @@@SNIPEND

  private final GreetingActivities activities =
      Workflow.newActivityStub(
          GreetingActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  public String greet(String name) {
    // @@@SNIPSTART java-opentelemetry-v2-workflow-counter
    GlobalOpenTelemetry.getMeter(INSTRUMENTATION_NAME)
        .counterBuilder("greeting.workflow.runs")
        .build()
        .add(1);
    // @@@SNIPEND

    logger.info("Requesting greeting for {}", name);

    // @@@SNIPSTART java-opentelemetry-v2-workflow-span
    Span span =
        GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME)
            .spanBuilder("request-greeting")
            .startSpan();
    try (Scope ignored = span.makeCurrent()) {
      return activities.composeGreeting(name);
    } finally {
      span.end();
    }
    // @@@SNIPEND
  }
}
