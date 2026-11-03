package io.temporal.docs.activities.execution;

import io.temporal.activity.ActivityCancellationType;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Map;

public class ActivityOptionsExample {

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String greet(String option);
    }

    /** Creates an Activity stub with options, as each "With ActivityStub" example shows. */
    public static class StubOptionsWorkflowImpl implements GreetingWorkflow {
        @Override
        public String greet(String option) {
            switch (option) {
                case "timeouts": {
                    // @@@SNIPSTART java-activity-execution-timeouts-stub
                    GreetingActivities activities = Workflow.newActivityStub(GreetingActivities.class,
                                    ActivityOptions.newBuilder()
                                            .setScheduleToCloseTimeout(Duration.ofSeconds(5))
                                            // .setStartToCloseTimeout(Duration.ofSeconds(2))
                                            // .setScheduleToStartTimeout(Duration.ofSeconds(20))
                                            .build());
                    // @@@SNIPEND
                    return activities.composeGreeting("Hello", option);
                }
                case "scheduleToClose": {
                    // @@@SNIPSTART java-activity-execution-schedule-to-close-stub
                    GreetingActivities activities = Workflow.newActivityStub(GreetingActivities.class,
                                    ActivityOptions.newBuilder()
                                            .setScheduleToCloseTimeout(Duration.ofSeconds(5))
                                            .build());
                    // @@@SNIPEND
                    return activities.composeGreeting("Hello", option);
                }
                case "scheduleToStart": {
                    // @@@SNIPSTART java-activity-execution-schedule-to-start-stub
                    GreetingActivities activities = Workflow.newActivityStub(GreetingActivities.class,
                                    ActivityOptions.newBuilder()
                                            .setScheduleToStartTimeout(Duration.ofSeconds(5))
                                            // note that either StartToCloseTimeout or ScheduleToCloseTimeout are
                                            // required when setting Activity options.
                                            .setScheduleToCloseTimeout(Duration.ofSeconds(20))
                                            .build());
                    // @@@SNIPEND
                    return activities.composeGreeting("Hello", option);
                }
                case "startToClose": {
                    // @@@SNIPSTART java-activity-execution-start-to-close-stub
                    GreetingActivities activities = Workflow.newActivityStub(GreetingActivities.class,
                                ActivityOptions.newBuilder()
                                        .setStartToCloseTimeout(Duration.ofSeconds(2))
                                        .build());
                    // @@@SNIPEND
                    return activities.composeGreeting("Hello", option);
                }
                default: {
                    // @@@SNIPSTART java-activity-execution-task-queue-stub
                    GreetingActivities activities = Workflow.newActivityStub(GreetingActivities.class,
                                    ActivityOptions.newBuilder()
                                            // note that either StartToCloseTimeout or ScheduleToCloseTimeout are required when
                                            // setting Activity options.
                                            .setStartToCloseTimeout(Duration.ofSeconds(5))
                                            .setTaskQueue("yourTaskQueue")
                                            .build());
                    // @@@SNIPEND
                    return activities.composeGreeting("Hello", option);
                }
            }
        }
    }

    public static class HeartbeatWorkflowImpl implements GreetingWorkflow {
        // @@@SNIPSTART java-activity-execution-heartbeat-stub
        private final GreetingActivities activities =
            Workflow.newActivityStub(
                GreetingActivities.class,
                ActivityOptions.newBuilder()
                    // note that either StartToCloseTimeout or ScheduleToCloseTimeout are
                    // required when setting Activity options.
                    .setStartToCloseTimeout(Duration.ofSeconds(5))
                    .setHeartbeatTimeout(Duration.ofSeconds(2))
                    .build());
        // @@@SNIPEND

        @Override
        public String greet(String option) {
            return activities.composeGreeting("Hello", option);
        }
    }

    public static class RetryOptionsWorkflowImpl implements GreetingWorkflow {
        // @@@SNIPSTART java-activity-execution-retry-stub
        private final ActivityOptions options =
            ActivityOptions.newBuilder()
                // note that either StartToCloseTimeout or ScheduleToCloseTimeout are
                // required when setting Activity options.
                .setStartToCloseTimeout(Duration.ofSeconds(5))
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(10))
                        .build())
                .build();
        // @@@SNIPEND

        private final GreetingActivities activities = Workflow.newActivityStub(GreetingActivities.class, options);

        @Override
        public String greet(String option) {
            return activities.composeGreeting("Hello", option);
        }
    }

    public static class CancellationTypeWorkflowImpl implements GreetingWorkflow {
        // @@@SNIPSTART java-activity-execution-cancellation-stub
        private final GreetingActivities activities =
          Workflow.newActivityStub(
              GreetingActivities.class,
              ActivityOptions.newBuilder()
                  // note that either StartToCloseTimeout or ScheduleToCloseTimeout are
                  // required when setting Activity options.
                  .setStartToCloseTimeout(Duration.ofSeconds(5))
                  .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                  .build());
        // @@@SNIPEND

        @Override
        public String greet(String option) {
            return activities.composeGreeting("Hello", option);
        }
    }

    /**
     * Creates the Activity stub without options, so the per-Activity-Type options in
     * WorkflowImplementationOptions apply.
     */
    public static class WorkflowImplementationOptionsWorkflowImpl implements GreetingWorkflow {
        private final GreetingActivities activities = Workflow.newActivityStub(GreetingActivities.class);

        @Override
        public String greet(String option) {
            return option.startsWith("get")
                ? activities.getCustomerGreeting(option)
                : activities.emailCustomerGreeting(option);
        }
    }

    public static WorkflowImplementationOptions timeouts() {
        // @@@SNIPSTART java-activity-execution-timeouts-wio
        WorkflowImplementationOptions options =
                    WorkflowImplementationOptions.newBuilder()
                            .setActivityOptions(
                                    Map.of(
                                            "GetCustomerGreeting",
                                            // Set Activity Execution timeout
                                            ActivityOptions.newBuilder()
                                                .setScheduleToCloseTimeout(Duration.ofSeconds(5))
                                                // .setStartToCloseTimeout(Duration.ofSeconds(2))
                                                // .setScheduleToStartTimeout(Duration.ofSeconds(5))
                                                .build()))
                            .build();
        // @@@SNIPEND
        return options;
    }

    public static WorkflowImplementationOptions scheduleToClose() {
        // @@@SNIPSTART java-activity-execution-schedule-to-close-wio
        WorkflowImplementationOptions options =
                    WorkflowImplementationOptions.newBuilder()
                            .setActivityOptions(
                                    Map.of(
                                            "GetCustomerGreeting",
                                            ActivityOptions.newBuilder()
                                                .setScheduleToCloseTimeout(Duration.ofSeconds(5))
                                                .build()))
                            .build();
        // @@@SNIPEND
        return options;
    }

    public static WorkflowImplementationOptions scheduleToStart() {
        // @@@SNIPSTART java-activity-execution-schedule-to-start-wio
        WorkflowImplementationOptions options =
                   WorkflowImplementationOptions.newBuilder()
                            .setActivityOptions(
                                    Map.of(
                                      "GetCustomerGreeting",
                                      ActivityOptions.newBuilder()
                                          .setScheduleToStartTimeout(Duration.ofSeconds(5))
                                          // note that either StartToCloseTimeout or ScheduleToCloseTimeout are
                                          // required when setting Activity options.
                                          .setStartToCloseTimeout(Duration.ofSeconds(5))
                                          .build()))
                            .build();
        // @@@SNIPEND
        return options;
    }

    public static WorkflowImplementationOptions startToClose() {
        // @@@SNIPSTART java-activity-execution-start-to-close-wio
        WorkflowImplementationOptions options =
                    WorkflowImplementationOptions.newBuilder()
                            .setActivityOptions(
                                    Map.of(
                                      "EmailCustomerGreeting",
                                            ActivityOptions.newBuilder()
                                                  // Set Activity Execution timeout (single run)
                                                  .setStartToCloseTimeout(Duration.ofSeconds(2))
                                                  .build()))
                            .build();
        // @@@SNIPEND
        return options;
    }

    public static WorkflowImplementationOptions heartbeat() {
        // @@@SNIPSTART java-activity-execution-heartbeat-wio
        WorkflowImplementationOptions options =
                    WorkflowImplementationOptions.newBuilder()
                            .setActivityOptions(
                                    Map.of(
                                      "EmailCustomerGreeting",
                                            ActivityOptions.newBuilder()
                                                // note that either StartToCloseTimeout or ScheduleToCloseTimeout are
                                                // required when setting Activity options.
                                                  .setStartToCloseTimeout(Duration.ofSeconds(5))
                                                  .setHeartbeatTimeout(Duration.ofSeconds(2))
                                                  .build()))
                            .build();
        // @@@SNIPEND
        return options;
    }

    public static WorkflowImplementationOptions taskQueue() {
        // @@@SNIPSTART java-activity-execution-task-queue-wio
        WorkflowImplementationOptions options =
                    WorkflowImplementationOptions.newBuilder()
                            .setActivityOptions(
                                    Map.of(
                                      "EmailCustomerGreeting",
                                            ActivityOptions.newBuilder()
                                                  // note that either StartToCloseTimeout or ScheduleToCloseTimeout are
                                                  // required when setting Activity options.
                                                  .setStartToCloseTimeout(Duration.ofSeconds(5))
                                                  .setTaskQueue("yourTaskQueue")
                                                  .build()))
                            .build();
        // @@@SNIPEND
        return options;
    }

    public static WorkflowImplementationOptions retry() {
        // @@@SNIPSTART java-activity-execution-retry-wio
        WorkflowImplementationOptions options =
                WorkflowImplementationOptions.newBuilder()
                       .setActivityOptions(
                            Map.of(
                                "EmailCustomerGreeting",
                                ActivityOptions.newBuilder()
                                      // note that either StartToCloseTimeout or ScheduleToCloseTimeout are
                                      // required when setting Activity options.
                                      .setStartToCloseTimeout(Duration.ofSeconds(5))
                                      .setRetryOptions(
                                            RetryOptions.newBuilder()
                                                .setDoNotRetry(NullPointerException.class.getName())
                                                .build())
                                      .build()))
                      .build();
        // @@@SNIPEND
        return options;
    }

    public static WorkflowImplementationOptions cancellation() {
        // @@@SNIPSTART java-activity-execution-cancellation-wio
        WorkflowImplementationOptions options =
                WorkflowImplementationOptions.newBuilder()
                       .setActivityOptions(
                            Map.of(
                                "EmailCustomerGreeting",
                                ActivityOptions.newBuilder()
                                      // note that either StartToCloseTimeout or ScheduleToCloseTimeout are
                                      // required when setting Activity options.
                                      .setStartToCloseTimeout(Duration.ofSeconds(5))
                                      .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                                      .build()))
                      .build();
        // @@@SNIPEND
        return options;
    }
}
