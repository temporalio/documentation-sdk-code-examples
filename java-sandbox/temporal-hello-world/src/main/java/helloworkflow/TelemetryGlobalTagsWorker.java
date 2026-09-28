package helloworkflow;

import java.util.HashMap;
import java.util.Map;

import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.StatsReporter;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

public class TelemetryGlobalTagsWorker {

  // task queue to be used for this sample
  public static final String DEFAULT_TASK_QUEUE_NAME = "metricsqueue";

  public static void main(String[] args) {

    // Set up prometheus registry and stats reported
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    StatsReporter reporter = new MicrometerClientStatsReporter(registry);

    Map<String, String> globalTags = new HashMap<>();
    globalTags.put("team", "content-platform");
    globalTags.put("service", "checkout");
    globalTags.put("cost_center", "cc-1042");
    globalTags.put("environment", "production");

    Scope scope = new RootScopeBuilder()
        .tags(globalTags)
        .reporter(reporter)
        .reportEvery(com.uber.m3.util.Duration.ofSeconds(10));

    WorkflowServiceStubsOptions stubOptions =
        WorkflowServiceStubsOptions.newBuilder().setMetricsScope(scope).build();
  }
}