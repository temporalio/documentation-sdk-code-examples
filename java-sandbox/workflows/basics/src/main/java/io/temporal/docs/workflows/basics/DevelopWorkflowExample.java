package io.temporal.docs.workflows.basics;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class DevelopWorkflowExample {

    public static class Arguments {
        public String name;

        public Arguments() {}

        public Arguments(String name) {
            this.name = name;
        }
    }

    // @@@SNIPSTART java-workflow-basics-interface
    // Workflow interface
    @WorkflowInterface
    public interface YourWorkflow {

        @WorkflowMethod
        String yourWFMethod(Arguments args);
    }
    // @@@SNIPEND

    @WorkflowInterface
    public interface GreetingWorkflow {

        @WorkflowMethod
        String getGreeting(String name);
    }

    // @@@SNIPSTART java-workflow-basics-implementation
    // Define the Workflow implementation which implements our getGreeting Workflow method.
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        @Override
        public String getGreeting(String name) {
            return "Hello " + name + "!";
        }
    }
    // @@@SNIPEND
}
