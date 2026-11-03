package io.temporal.docs.workflows.childworkflows;

import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class ParallelChildrenExample {

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreeting(String name);
    }

    @WorkflowInterface
    public interface GreetingChild {
        @WorkflowMethod
        String composeGreeting(String greeting, String name);
    }

    public static class GreetingChildImpl implements GreetingChild {
        @Override
        public String composeGreeting(String greeting, String name) {
            return greeting + " " + name + "!";
        }
    }

    // @@@SNIPSTART java-child-workflows-parallel
    // Parent Workflow implementation
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        @Override
        public String getGreeting(String name) {

            // Workflows are stateful, so a new stub must be created for each new child.
            GreetingChild child1 = Workflow.newChildWorkflowStub(GreetingChild.class);
            Promise<String> greeting1 = Async.function(child1::composeGreeting, "Hello", name);

            // Both children will run concurrently.
            GreetingChild child2 = Workflow.newChildWorkflowStub(GreetingChild.class);
            Promise<String> greeting2 = Async.function(child2::composeGreeting, "Bye", name);

            // Do something else here.

            return "First: " + greeting1.get() + ", second: " + greeting2.get();
        }
    }
    // @@@SNIPEND
}
