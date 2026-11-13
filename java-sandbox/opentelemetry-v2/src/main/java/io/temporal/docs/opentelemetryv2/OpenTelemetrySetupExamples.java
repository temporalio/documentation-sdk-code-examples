package io.temporal.docs.opentelemetryv2;

import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.temporal.client.WorkflowClient;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.opentelemetry.v2.OpenTelemetryPlugin;
import io.temporal.opentelemetry.v2.ReplaySafeOpenTelemetry;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;

class OpenTelemetrySetupExamples {
  static ReplaySafeOpenTelemetry createOpenTelemetry() {
    // @@@SNIPSTART java-opentelemetry-v2-setup
    // Batch processors perform exporter I/O outside the Workflow thread.
    ReplaySafeOpenTelemetry openTelemetry =
        ReplaySafeOpenTelemetry.newBuilder()
            .setTracerProviderBuilder(
                SdkTracerProvider.builder()
                    .addSpanProcessor(
                        BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build()).build()))
            .setMeterProviderBuilder(
                SdkMeterProvider.builder()
                    .registerMetricReader(
                        PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().build())
                            .build()))
            .setLoggerProviderBuilder(
                SdkLoggerProvider.builder()
                    .addLogRecordProcessor(
                        BatchLogRecordProcessor.builder(
                                OtlpGrpcLogRecordExporter.builder().build())
                            .build()))
            .build();
    GlobalOpenTelemetry.set(openTelemetry);
    // @@@SNIPEND
    return openTelemetry;
  }

  static WorkerFactory addPlugin() {
    // @@@SNIPSTART java-opentelemetry-v2-plugin
    OpenTelemetryPlugin plugin = OpenTelemetryPlugin.newBuilder().build();

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setPlugins(plugin).build());
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // @@@SNIPEND
    return factory;
  }

  static OpenTelemetryPlugin addTemporalSpans() {
    // @@@SNIPSTART java-opentelemetry-v2-temporal-spans
    OpenTelemetryPlugin plugin = OpenTelemetryPlugin.newBuilder().setAddTemporalSpans(true).build();
    // @@@SNIPEND
    return plugin;
  }

  static WorkflowServiceStubs exportSdkMetrics(ReplaySafeOpenTelemetry openTelemetry) {
    // @@@SNIPSTART java-opentelemetry-v2-sdk-metrics
    MeterRegistry registry = OpenTelemetryMeterRegistry.builder(openTelemetry).build();
    Scope scope =
        new RootScopeBuilder()
            .reporter(new MicrometerClientStatsReporter(registry))
            .reportEvery(com.uber.m3.util.Duration.ofSeconds(10));

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setMetricsScope(scope).build());
    // @@@SNIPEND
    return service;
  }

  static void installLogAppender(ReplaySafeOpenTelemetry openTelemetry) {
    // @@@SNIPSTART java-opentelemetry-v2-log-appender
    OpenTelemetryAppender.install(openTelemetry);
    // @@@SNIPEND
  }

  static void shutdown(WorkerFactory factory, ReplaySafeOpenTelemetry openTelemetry) {
    // @@@SNIPSTART java-opentelemetry-v2-shutdown
    factory.shutdown();
    openTelemetry.close();
    // @@@SNIPEND
  }
}
