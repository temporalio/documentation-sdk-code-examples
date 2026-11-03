package io.temporal.docs.workflows.schedules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.schedules.Schedule;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleClientOptions;
import io.temporal.client.schedules.ScheduleDescription;
import io.temporal.client.schedules.ScheduleException;
import io.temporal.client.schedules.ScheduleHandle;
import io.temporal.client.schedules.ScheduleIntervalSpec;
import io.temporal.client.schedules.ScheduleOptions;
import io.temporal.client.schedules.ScheduleSpec;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The in-memory test server doesn't support Schedules, so these tests need a dev server, started
 * with the extra Namespace that the create example uses:
 *
 * <pre>temporal server start-dev --namespace custom_namespace</pre>
 *
 * Set TEMPORAL_ADDRESS to use an address other than localhost:7233. The tests are skipped when no
 * server is reachable.
 */
public class SchedulesTest {

    private static WorkflowServiceStubs service;
    private static WorkflowClient client;
    private static ScheduleClient scheduleClient;
    private static WorkerFactory factory;

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

        client = WorkflowClient.newInstance(service);
        scheduleClient = ScheduleClient.newInstance(service);
        factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker("TaskQueue");
        worker.registerWorkflowImplementationTypes(HelloSchedules.GreetingWorkflowImpl.class);
        factory.newWorker(CronAndStartDelayExample.TASK_QUEUE)
            .registerWorkflowImplementationTypes(HelloSchedules.GreetingWorkflowImpl.class);
        factory.start();
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

    private static Schedule paused5sSchedule() {
        return Schedule.newBuilder()
            .setAction(ScheduleActionStartWorkflow.newBuilder()
                .setWorkflowType(HelloSchedules.GreetingWorkflow.class)
                .setArguments("World")
                .setOptions(WorkflowOptions.newBuilder()
                    .setWorkflowId("scheduled-" + UUID.randomUUID())
                    .setTaskQueue("TaskQueue")
                    .build())
                .build())
            .setSpec(ScheduleSpec.newBuilder()
                .setIntervals(Collections.singletonList(new ScheduleIntervalSpec(Duration.ofHours(1))))
                .build())
            .build();
    }

    @Test
    public void createScheduleInCustomNamespace() {
        ScheduleHandle handle = SchedulesExample.createSchedule(service);
        try {
            ScheduleClient custom = ScheduleClient.newInstance(service,
                ScheduleClientOptions.newBuilder().setNamespace("custom_namespace").build());
            assertEquals(Duration.ofSeconds(5), custom.getHandle("ScheduleId").describe()
                .getSchedule().getSpec().getIntervals().get(0).getEvery());
        } finally {
            handle.delete();
        }
    }

    @Test
    public void manageSchedule() throws Exception {
        scheduleClient.createSchedule("schedule-id", paused5sSchedule(), ScheduleOptions.newBuilder().build());
        try {
            SchedulesExample.pause(scheduleClient);
            ScheduleDescription description = SchedulesExample.describe(scheduleClient);
            assertTrue(description.getSchedule().getState().isPaused());
            assertEquals("Pausing the schedule for now", description.getSchedule().getState().getNote());

            SchedulesExample.update(scheduleClient);
            description = SchedulesExample.describe(scheduleClient);
            assertTrue(description.getSchedule().getState().isLimitedAction());
            assertEquals(10, description.getSchedule().getState().getRemainingActions());

            SchedulesExample.trigger(scheduleClient);
            SchedulesExample.backfill(scheduleClient);
            // Trigger runs even while the Schedule is paused.
            long deadline = System.currentTimeMillis() + 10_000;
            while (SchedulesExample.describe(scheduleClient).getInfo().getNumActions() < 1
                && System.currentTimeMillis() < deadline) {
                Thread.sleep(200);
            }
            assertTrue(SchedulesExample.describe(scheduleClient).getInfo().getNumActions() >= 1);

            deadline = System.currentTimeMillis() + 10_000;
            boolean listed = false;
            while (!listed && System.currentTimeMillis() < deadline) {
                listed = SchedulesExample.list(scheduleClient).anyMatch(s -> s.getScheduleId().equals("schedule-id"));
                if (!listed) {
                    Thread.sleep(200);
                }
            }
            assertTrue(listed);
        } finally {
            SchedulesExample.delete(scheduleClient);
        }
        assertThrows(ScheduleException.class, () -> SchedulesExample.describe(scheduleClient));
    }

    @Test
    public void cronSchedule() {
        HelloSchedules.GreetingWorkflow workflow = CronAndStartDelayExample.cronWorkflow(client);
        WorkflowClient.start(workflow::greet, "World");
        WorkflowStub stub = WorkflowStub.fromTyped(workflow);
        try {
            String cronSchedule = client.fetchHistory("YourWF").getEvents().get(0)
                .getWorkflowExecutionStartedEventAttributes().getCronSchedule();
            assertEquals("* * * * *", cronSchedule);
        } finally {
            stub.terminate("test finished");
        }
    }

    @Test
    public void startDelay() {
        HelloSchedules.GreetingWorkflow workflow = CronAndStartDelayExample.delayedWorkflow(client);
        WorkflowClient.start(workflow::greet, "World");
        try {
            WorkflowExecutionInfo info =
                client.newUntypedWorkflowStub("YourWorkflow").describe().getWorkflowExecutionInfo();
            long delaySeconds = info.getExecutionTime().getSeconds() - info.getStartTime().getSeconds();
            assertEquals(Duration.ofHours(12).getSeconds(), delaySeconds, 2);
        } finally {
            WorkflowStub.fromTyped(workflow).terminate("test finished");
        }
    }
}
