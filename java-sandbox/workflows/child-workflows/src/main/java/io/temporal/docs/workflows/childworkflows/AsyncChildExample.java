package io.temporal.docs.workflows.childworkflows;

import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.ChildWorkflowStub;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class AsyncChildExample {

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

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        @Override
        public String getGreeting(String name) {
            // @@@SNIPSTART java-child-workflows-async
            GreetingChild child = Workflow.newChildWorkflowStub(GreetingChild.class);
            Promise<String> greeting = Async.function(child::composeGreeting, "Hello", name);
            // ...
            return greeting.get();
            // @@@SNIPEND
        }
    }

    public static class UntypedGreetingWorkflowImpl implements GreetingWorkflow {
        @Override
        public String getGreeting(String name) {
            // @@@SNIPSTART java-child-workflows-async-untyped
            ChildWorkflowStub childUntyped =
                Workflow.newUntypedChildWorkflowStub(
                    "GreetingChild", // your workflow type
                    ChildWorkflowOptions.newBuilder().setWorkflowId("childWorkflow").build());

            Promise<String> greeting =
                childUntyped.executeAsync(String.class, String.class, "Hello", name);
            String result = greeting.get();
            // @@@SNIPEND
            return result;
        }
    }
}
