package io.temporal.docs.workflows.timeouts;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import java.time.Duration;

public class TimeoutsExample {

    static final String TASK_QUEUE = "timeouts";

    public static GreetWorkflowInterface withExecutionTimeout(WorkflowClient client) {
        // @@@SNIPSTART java-workflow-timeouts-execution-timeout
        // Create a Workflow stub for GreetWorkflowInterface
        GreetWorkflowInterface workflow1 =
            client.newWorkflowStub(
                GreetWorkflowInterface.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("YourWorkflow")
                    .setTaskQueue(TASK_QUEUE)
                    // Set Workflow Timeout duration
                    .setWorkflowExecutionTimeout(Duration.ofSeconds(10))
                    // .setWorkflowRunTimeout(Duration.ofSeconds(10))
                    // .setWorkflowTaskTimeout(Duration.ofSeconds(10))
                    .build());
        // @@@SNIPEND
        return workflow1;
    }

    public static GreetWorkflowInterface withRetryOptions(WorkflowClient client) {
        // @@@SNIPSTART java-workflow-timeouts-retry-options
        // Create a Workflow stub for GreetWorkflowInterface
        GreetWorkflowInterface workflow1 =
            client.newWorkflowStub(
                GreetWorkflowInterface.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("GreetWF")
                    .setTaskQueue(TASK_QUEUE)
                    // Set Workflow Retry Options
                    .setRetryOptions(
                        RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumAttempts(3)
                            .build())
                    .build());
        // @@@SNIPEND
        return workflow1;
    }
}
