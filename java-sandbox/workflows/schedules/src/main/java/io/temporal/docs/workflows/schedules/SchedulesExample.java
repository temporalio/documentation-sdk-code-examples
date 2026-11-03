package io.temporal.docs.workflows.schedules;

import io.temporal.client.WorkflowOptions;
import io.temporal.client.schedules.Schedule;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;
import io.temporal.client.schedules.ScheduleBackfill;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleClientOptions;
import io.temporal.client.schedules.ScheduleDescription;
import io.temporal.client.schedules.ScheduleHandle;
import io.temporal.client.schedules.ScheduleIntervalSpec;
import io.temporal.client.schedules.ScheduleListDescription;
import io.temporal.client.schedules.ScheduleOptions;
import io.temporal.client.schedules.ScheduleSpec;
import io.temporal.client.schedules.ScheduleState;
import io.temporal.client.schedules.ScheduleUpdate;
import io.temporal.client.schedules.ScheduleUpdateInput;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

public class SchedulesExample {

    public static ScheduleHandle createSchedule(WorkflowServiceStubs service) {
        // @@@SNIPSTART java-schedules-create
        Schedule schedule =
            Schedule.newBuilder()
                .setAction(
                    ScheduleActionStartWorkflow.newBuilder()
                        .setWorkflowType(HelloSchedules.GreetingWorkflow.class)
                        .setArguments("World")
                        .setOptions(
                            WorkflowOptions.newBuilder()
                                .setWorkflowId("WorkflowId")
                                .setTaskQueue("TaskQueue")
                                .build())
                        .build())
                .setSpec(ScheduleSpec.newBuilder()
                        .setIntervals(
                            Collections.singletonList(new ScheduleIntervalSpec(Duration.ofSeconds(5)))
                        ).build()
                    )
                .build();

        // Create a schedule on the server
        ScheduleClient scheduleClient = ScheduleClient
            .newInstance(service, ScheduleClientOptions.newBuilder()
                .setNamespace("custom_namespace")
                .build()
            );

        ScheduleHandle handle =
            scheduleClient.createSchedule("ScheduleId", schedule, ScheduleOptions.newBuilder().build());
        // @@@SNIPEND
        return handle;
    }

    public static void backfill(ScheduleClient client) {
        // @@@SNIPSTART java-schedules-backfill
        ScheduleHandle handle = client.getHandle("schedule-id");

        Instant now = Instant.now();
        handle.backfill(
            Arrays.asList(
                new ScheduleBackfill(now.minusMillis(5500), now.minusMillis(2500)),
                new ScheduleBackfill(now.minusMillis(2500), now)));
        // @@@SNIPEND
    }

    public static void delete(ScheduleClient client) {
        // @@@SNIPSTART java-schedules-delete
        ScheduleHandle handle = client.getHandle("schedule-id");
        handle.delete();
        // @@@SNIPEND
    }

    public static ScheduleDescription describe(ScheduleClient client) {
        // @@@SNIPSTART java-schedules-describe
        ScheduleHandle handle = client.getHandle("schedule-id");
        ScheduleDescription description = handle.describe();
        // @@@SNIPEND
        return description;
    }

    public static Stream<ScheduleListDescription> list(ScheduleClient client) {
        // @@@SNIPSTART java-schedules-list
        Stream<ScheduleListDescription> scheduleStream = client.listSchedules();
        // @@@SNIPEND
        return scheduleStream;
    }

    public static void pause(ScheduleClient client) {
        // @@@SNIPSTART java-schedules-pause
        ScheduleHandle handle = client.getHandle("schedule-id");
        handle.pause("Pausing the schedule for now");
        // @@@SNIPEND
    }

    public static void trigger(ScheduleClient client) {
        // @@@SNIPSTART java-schedules-trigger
        ScheduleHandle handle = client.getHandle("schedule-id");
        handle.trigger();
        // @@@SNIPEND
    }

    public static void update(ScheduleClient client) {
        // @@@SNIPSTART java-schedules-update
        ScheduleHandle handle = client.getHandle("schedule-id");
        handle.update(
            (ScheduleUpdateInput input) -> {
              Schedule.Builder builder = Schedule.newBuilder(input.getDescription().getSchedule());
              // Limit the Schedule to 10 more Actions
              builder.setState(
                  ScheduleState.newBuilder()
                      .setLimitedAction(true)
                      .setRemainingActions(10)
                      .build());
              return new ScheduleUpdate(builder.build());
            });
        // @@@SNIPEND
    }
}
