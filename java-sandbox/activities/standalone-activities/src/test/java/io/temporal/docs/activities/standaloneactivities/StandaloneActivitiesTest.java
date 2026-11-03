package io.temporal.docs.activities.standaloneactivities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.StartActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The in-memory test server doesn't run Standalone Activities, so these tests need a dev server
 * ({@code temporal server start-dev}). Set TEMPORAL_ADDRESS to use an address other than
 * localhost:7233. The tests are skipped when no server is reachable.
 */
public class StandaloneActivitiesTest {

    private static WorkflowServiceStubs service;
    private static WorkerFactory factory;
    private static ActivityClient client;

    @BeforeAll
    public static void setUp() {
        String address = System.getenv().getOrDefault("TEMPORAL_ADDRESS", "localhost:7233");
        service = WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setTarget(address).build());
        boolean reachable;
        try {
            service.healthCheck();
            reachable = true;
        } catch (RuntimeException e) {
            reachable = false;
        }
        assumeTrue(reachable, "No Temporal Service at " + address);

        factory = WorkerFactory.newInstance(WorkflowClient.newInstance(service));
        Worker worker = factory.newWorker(StandaloneActivityWorker.TASK_QUEUE);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        factory.start();
        client = ActivityClient.newInstance(service, ActivityClientOptions.newBuilder().setNamespace("default").build());
    }

    @AfterAll
    public static void tearDown() {
        if (factory != null) {
            factory.shutdownNow();
        }
        if (service != null) {
            service.shutdownNow();
        }
    }

    private static StartActivityOptions options() {
        return StartActivityOptions.newBuilder()
            .setId("test-" + UUID.randomUUID())
            .setTaskQueue(StandaloneActivityWorker.TASK_QUEUE)
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Test
    public void executeWithMethodReference() {
        assertEquals("Hello, World!", client.execute(
            GreetingActivities.class, GreetingActivities::composeGreeting, options(), "Hello", "World"));
    }

    @Test
    public void executeByTypeName() {
        assertEquals("Hello, World!",
            client.execute("ComposeGreeting", String.class, options(), "Hello", "World"));
    }
}
