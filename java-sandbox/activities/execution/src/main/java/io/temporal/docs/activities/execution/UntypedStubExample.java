package io.temporal.docs.activities.execution;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

public class UntypedStubExample {

    @WorkflowInterface
    public interface UntypedGreetingWorkflow {
        @WorkflowMethod
        String greet();
    }

    public static class UntypedGreetingWorkflowImpl implements UntypedGreetingWorkflow {
        @Override
        public String greet() {
            // @@@SNIPSTART java-activity-execution-untyped-stub
            // Workflow code
            ActivityOptions activityOptions =
                ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(3))
                    .setTaskQueue("simple-queue-node")
                    .build();

            ActivityStub activity = Workflow.newUntypedActivityStub(activityOptions);
            String result = activity.execute("ComposeGreeting", String.class, "Hello World", "Spanish");
            // @@@SNIPEND
            return result;
        }
    }
}
