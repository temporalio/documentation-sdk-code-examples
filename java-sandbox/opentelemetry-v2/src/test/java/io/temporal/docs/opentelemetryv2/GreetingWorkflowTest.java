package io.temporal.docs.opentelemetryv2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.temporal.client.WorkflowOptions;
import io.temporal.opentelemetry.v2.OpenTelemetryPlugin;
import io.temporal.opentelemetry.v2.ReplaySafeOpenTelemetry;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GreetingWorkflowTest {
  private static final String TASK_QUEUE = "opentelemetry-v2-test";

  private static final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
  private static final InMemoryMetricReader metricReader = InMemoryMetricReader.create();
  private static final InMemoryLogRecordExporter logExporter = InMemoryLogRecordExporter.create();
  private static ReplaySafeOpenTelemetry openTelemetry;

  @BeforeAll
  static void registerOpenTelemetry() {
    openTelemetry =
        ReplaySafeOpenTelemetry.newBuilder()
            .setTracerProviderBuilder(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter)))
            .setMeterProviderBuilder(SdkMeterProvider.builder().registerMetricReader(metricReader))
            .setLoggerProviderBuilder(
                SdkLoggerProvider.builder()
                    .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter)))
            .build();
    GlobalOpenTelemetry.set(openTelemetry);
    OpenTelemetryAppender.install(openTelemetry);
  }

  @AfterAll
  static void resetOpenTelemetry() {
    GlobalOpenTelemetry.resetForTest();
    openTelemetry.close();
  }

  @Test
  void emitsSpansMetricsAndLogs() {
    TestEnvironmentOptions options =
        TestEnvironmentOptions.newBuilder()
            .setWorkflowServiceStubsOptions(
                WorkflowServiceStubsOptions.newBuilder()
                    .setPlugins(OpenTelemetryPlugin.newBuilder().setAddTemporalSpans(true).build())
                    .build())
            .build();
    try (TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance(options)) {
      Worker worker = testEnv.newWorker(TASK_QUEUE);
      worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
      worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
      testEnv.start();

      GreetingWorkflow workflow =
          testEnv
              .getWorkflowClient()
              .newWorkflowStub(
                  GreetingWorkflow.class,
                  WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

      assertEquals("Hello, Temporal!", workflow.greet("Temporal"));
    }

    Map<String, SpanData> spans =
        spanExporter.getFinishedSpanItems().stream()
            .collect(Collectors.toMap(SpanData::getName, Function.identity(), (a, b) -> a));
    SpanData runWorkflow = spans.get("RunWorkflow:GreetingWorkflow");
    SpanData requestGreeting = spans.get("request-greeting");
    SpanData runActivity = spans.get("RunActivity:ComposeGreeting");
    SpanData formatGreeting = spans.get("format-greeting");
    assertEquals(runWorkflow.getSpanId(), requestGreeting.getParentSpanId());
    assertEquals(runActivity.getSpanId(), formatGreeting.getParentSpanId());
    assertEquals(runWorkflow.getTraceId(), formatGreeting.getTraceId());

    MetricData counter =
        metricReader.collectAllMetrics().stream()
            .filter(metric -> metric.getName().equals("greeting.workflow.runs"))
            .findFirst()
            .orElseThrow();
    assertEquals(1, counter.getLongSumData().getPoints().iterator().next().getValue());

    List<LogRecordData> logs = logExporter.getFinishedLogRecordItems();
    LogRecordData workflowLog =
        logs.stream()
            .filter(log -> log.getBodyValue().asString().equals("Requesting greeting for Temporal"))
            .findFirst()
            .orElseThrow();
    assertEquals(runWorkflow.getSpanId(), workflowLog.getSpanContext().getSpanId());
    assertTrue(
        logs.stream()
            .anyMatch(
                log -> log.getBodyValue().asString().equals("Formatting greeting for Temporal")));
  }
}
