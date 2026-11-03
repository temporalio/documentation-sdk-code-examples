package io.temporal.docs.workflows.childworkflows;

import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class SignalChildExample {

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreeting(String name);
    }

    // @@@SNIPSTART java-child-workflows-signal
    // Child Workflow interface
    @WorkflowInterface
    public interface GreetingChild {
        @WorkflowMethod
        String composeGreeting(String greeting, String name);

        @SignalMethod
        void updateName(String name);
    }

    // Parent Workflow implementation
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        @Override
        public String getGreeting(String name) {
            GreetingChild child = Workflow.newChildWorkflowStub(GreetingChild.class);
            Promise<String> greeting = Async.function(child::composeGreeting, "Hello", name);
            child.updateName("Temporal");
            return greeting.get();
        }
    }
    // @@@SNIPEND

    public static class GreetingChildImpl implements GreetingChild {
        private String name;
        private boolean nameUpdated;

        @Override
        public String composeGreeting(String greeting, String name) {
            this.name = name;
            Workflow.await(() -> nameUpdated);
            return greeting + " " + this.name + "!";
        }

        @Override
        public void updateName(String name) {
            this.name = name;
            nameUpdated = true;
        }
    }
}
