package io.temporal.docs.workflows.schedules;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.docs.workflows.schedules.HelloSchedules.GreetingWorkflow;
import java.time.Duration;

public class CronAndStartDelayExample {

    static final String TASK_QUEUE = "schedules";

    public static GreetingWorkflow cronWorkflow(WorkflowClient client) {
        // @@@SNIPSTART java-schedules-cron
        // Create a Workflow stub for GreetingWorkflow
        GreetingWorkflow workflow1 =
            client.newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("YourWF")
                    .setTaskQueue(TASK_QUEUE)
                    // Set Cron Schedule
                    .setCronSchedule("* * * * *")
                    .build());
        // @@@SNIPEND
        return workflow1;
    }

    public static GreetingWorkflow delayedWorkflow(WorkflowClient client) {
        // @@@SNIPSTART java-schedules-start-delay
        // Create a Workflow stub for GreetingWorkflow
        GreetingWorkflow workflow1 =
            client.newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("YourWorkflow")
                    .setTaskQueue(TASK_QUEUE)
                    // Start the workflow in 12 hours
                    .setStartDelay(Duration.ofHours(12))
                    .build());
        // @@@SNIPEND
        return workflow1;
    }
}
