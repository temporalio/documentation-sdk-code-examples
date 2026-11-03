package io.temporal.docs.workflows.childworkflows;

import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class SpawnChildExample {

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreeting(String name);
    }

    // @@@SNIPSTART java-child-workflows-spawn
    // Child Workflow interface
    @WorkflowInterface
    public interface GreetingChild {
        @WorkflowMethod
        String composeGreeting(String greeting, String name);
    }
    // Child Workflow implementation not shown

    // Parent Workflow implementation
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        @Override
        public String getGreeting(String name) {
            GreetingChild child = Workflow.newChildWorkflowStub(GreetingChild.class);

            // This is a blocking call that returns only after child has completed.
            return child.composeGreeting("Hello", name);
        }
    }
    // @@@SNIPEND

    public static class GreetingChildImpl implements GreetingChild {
        @Override
        public String composeGreeting(String greeting, String name) {
            return greeting + " " + name + "!";
        }
    }
}
