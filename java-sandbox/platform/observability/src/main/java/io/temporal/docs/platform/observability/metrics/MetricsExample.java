package io.temporal.docs.platform.observability.metrics;

import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.StatsReporter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.util.HashMap;
import java.util.Map;

public class MetricsExample {

    /** The registry to scrape, plus the stub options that report into it. */
    public record MetricsSetup(
        PrometheusMeterRegistry registry, WorkflowServiceStubsOptions stubOptions) {}

    public static MetricsSetup createStubOptions() {
        // @@@SNIPSTART java-observability-metrics-scope
        // See the Micrometer documentation for configuring other supported monitoring systems.
        // This example sets up a Prometheus registry and stats reporter.
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        StatsReporter reporter = new MicrometerClientStatsReporter(registry);

        // Set up a new scope that reports every 10 seconds.
        Scope scope = new RootScopeBuilder()
            .reporter(reporter)
            .reportEvery(com.uber.m3.util.Duration.ofSeconds(10));

        // For Prometheus collection, expose a scrape endpoint that serves registry.scrape().

        // Add the metrics scope to the WorkflowServiceStubs options.
        WorkflowServiceStubsOptions stubOptions =
            WorkflowServiceStubsOptions.newBuilder().setMetricsScope(scope).build();
        // @@@SNIPEND
        return new MetricsSetup(registry, stubOptions);
    }

    public static MetricsSetup createStubOptionsWithGlobalTags() {
        // @@@SNIPSTART java-observability-metrics-global-tags
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
        // @@@SNIPEND
        return new MetricsSetup(registry, stubOptions);
    }
}
