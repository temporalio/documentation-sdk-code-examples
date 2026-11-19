package io.temporal.docs.opentelemetryv2;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreetingActivitiesImpl implements GreetingActivities {
  private static final Logger logger = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

  @Override
  public String composeGreeting(String name) {
    logger.info("Formatting greeting for {}", name);

    Span span =
        GlobalOpenTelemetry.getTracer(GreetingWorkflowImpl.INSTRUMENTATION_NAME)
            .spanBuilder("format-greeting")
            .startSpan();
    try (Scope ignored = span.makeCurrent()) {
      return "Hello, " + name + "!";
    } finally {
      span.end();
    }
  }
}
