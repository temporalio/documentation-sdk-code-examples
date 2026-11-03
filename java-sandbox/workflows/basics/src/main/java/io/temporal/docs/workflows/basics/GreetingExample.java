package io.temporal.docs.workflows.basics;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

// @@@SNIPSTART java-workflow-basics-constructor
public class GreetingExample {
    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreeting(String input);

        @UpdateMethod
        boolean checkTitleValidity();
    }

    @ActivityInterface
    public interface TitleActivities {
        boolean checkTitleValidity(String nameWithTitle);
    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        private final String nameWithTitle;
        private boolean titleHasBeenChecked;

        private final TitleActivities activity = Workflow.newActivityStub(
            TitleActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .build());

        // Note the annotation is on a public constructor
        @WorkflowInit
        public GreetingWorkflowImpl(String input) {
          this.nameWithTitle = "Knight " + input;
          this.titleHasBeenChecked = false;
        }

        @Override
        public String getGreeting(String input) {
          Workflow.await(() -> titleHasBeenChecked);
          return "Hello " + nameWithTitle;
        }

        @Override
        public boolean checkTitleValidity() {
          // The handler can read nameWithTitle because the constructor
          // has already processed the Workflow input.
          boolean isValid = activity.checkTitleValidity(nameWithTitle);
          titleHasBeenChecked = true;
          return isValid;
        }
    }
}
// @@@SNIPEND
