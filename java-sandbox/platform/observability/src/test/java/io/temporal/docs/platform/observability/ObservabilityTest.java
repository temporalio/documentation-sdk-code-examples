package io.temporal.docs.platform.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.uber.m3.tally.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.docs.platform.observability.logging.GreetingWorkflow;
import io.temporal.docs.platform.observability.logging.GreetingWorkflowImpl;
import io.temporal.docs.platform.observability.metrics.MetricsExample;
import io.temporal.docs.platform.observability.searchattributes.Constants;
import io.temporal.docs.platform.observability.searchattributes.PizzaActivitiesImpl;
import io.temporal.docs.platform.observability.searchattributes.PizzaWorkflow;
import io.temporal.docs.platform.observability.searchattributes.PizzaWorkflowImpl;
import io.temporal.docs.platform.observability.searchattributes.Starter;
import io.temporal.docs.platform.observability.tracing.TracingExample;
import io.temporal.opentracing.OpenTracingOptions;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ObservabilityTest {

    private static final String TASK_QUEUE = "observability-test";

    private static String runGreeting(WorkflowClient client) {
        GreetingWorkflow workflow = client.newWorkflowStub(
            GreetingWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
        return workflow.greet("World");
    }

    private static String scrapeAfterGreeting(MetricsExample.MetricsSetup setup) throws Exception {
        Scope scope = setup.stubOptions().getMetricsScope();
        TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance(
            TestEnvironmentOptions.newBuilder().setMetricsScope(scope).build());
        try {
            env.newWorker(TASK_QUEUE).registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
            env.start();
            assertEquals("Hello, World!", runGreeting(env.getWorkflowClient()));
        } finally {
            env.close();
        }
        // Closing the scope flushes the buffered metrics to the registry.
        scope.close();
        return setup.registry().scrape();
    }

    @Test
    public void metricsReachThePrometheusRegistry() throws Exception {
        String scrape = scrapeAfterGreeting(MetricsExample.createStubOptions());
        assertTrue(scrape.contains("temporal_workflow_completed"), scrape);
    }

    @Test
    public void globalTagsAreOnEveryMetric() throws Exception {
        String scrape = scrapeAfterGreeting(MetricsExample.createStubOptionsWithGlobalTags());
        List<String> temporalSeries = scrape.lines()
            .filter(l -> l.startsWith("temporal_"))
            .collect(Collectors.toList());
        assertFalse(temporalSeries.isEmpty());
        for (String series : temporalSeries) {
            assertTrue(series.contains("team=\"content-platform\""), series);
            assertTrue(series.contains("environment=\"production\""), series);
        }
    }

    @Test
    public void tracingInterceptorsReportSpans() {
        MockTracer tracer = new MockTracer();
        OpenTracingOptions tracingOptions = TracingExample.tracingOptions(tracer);
        TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance();
        try {
            WorkflowClient client =
                TracingExample.createClient(env.getWorkflowServiceStubs(), tracingOptions);
            WorkerFactory factory = TracingExample.createWorkerFactory(client, tracingOptions);
            factory.newWorker(TASK_QUEUE).registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
            factory.start();
            assertEquals("Hello, World!", runGreeting(client));
            factory.shutdown();
        } finally {
            env.close();
        }
        List<String> operations = tracer.finishedSpans().stream()
            .map(MockSpan::operationName)
            .collect(Collectors.toList());
        assertTrue(operations.contains("StartWorkflow:GreetingWorkflow"), operations.toString());
        assertTrue(operations.contains("RunWorkflow:GreetingWorkflow"), operations.toString());
    }

    private static TestWorkflowEnvironment pizzaEnv() {
        TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance();
        env.registerSearchAttribute("isOrderFailed", IndexedValueType.INDEXED_VALUE_TYPE_BOOL);
        Worker worker = env.newWorker(Constants.TASK_QUEUE_NAME);
        worker.registerWorkflowImplementationTypes(PizzaWorkflowImpl.class);
        worker.registerActivitiesImplementations(new PizzaActivitiesImpl());
        env.start();
        return env;
    }

    private static WorkflowOptions starterOptions(String workflowId) {
        // Same options the Starter builds.
        return WorkflowOptions.newBuilder()
            .setWorkflowId(workflowId)
            .setTaskQueue(Constants.TASK_QUEUE_NAME)
            .setTypedSearchAttributes(Starter.generateSearchAttributes())
            .build();
    }

    @Test
    public void searchAttributeIsSetOnStartAndRemovedOnSuccess() {
        TestWorkflowEnvironment env = pizzaEnv();
        try {
            WorkflowClient client = env.getWorkflowClient();
            PizzaWorkflow workflow = client.newWorkflowStub(PizzaWorkflow.class, starterOptions("ok"));
            WorkflowClient.start(workflow::orderPizza, "701 Mission Street");

            assertTrue(client.fetchHistory("ok").getEvents().get(0)
                .getWorkflowExecutionStartedEventAttributes().getSearchAttributes()
                .containsIndexedFields("isOrderFailed"));
            assertEquals(5, WorkflowStub.fromTyped(workflow).getResult(Integer.class));

            long upserts = client.fetchHistory("ok").getEvents().stream()
                .filter(e -> e.getEventType() == EventType.EVENT_TYPE_UPSERT_WORKFLOW_SEARCH_ATTRIBUTES)
                .count();
            assertEquals(2, upserts);
            assertNull(WorkflowStub.fromTyped(workflow).describe()
                .getTypedSearchAttributes().get(Constants.IS_ORDER_FAILED));
        } finally {
            env.close();
        }
    }

    @Test
    public void searchAttributeIsTrueWhenTheOrderFails() {
        TestWorkflowEnvironment env = pizzaEnv();
        try {
            WorkflowClient client = env.getWorkflowClient();
            PizzaWorkflow workflow = client.newWorkflowStub(PizzaWorkflow.class, starterOptions("bad"));
            assertThrows(WorkflowFailedException.class, () -> workflow.orderPizza(""));
            assertEquals(true, WorkflowStub.fromTyped(workflow).describe()
                .getTypedSearchAttributes().get(Constants.IS_ORDER_FAILED));
        } finally {
            env.close();
        }
    }
}
