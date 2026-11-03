package io.temporal.docs.workflows.schedules;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class HelloSchedules {

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String greet(String name);
    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        @Override
        public String greet(String name) {
            return "Hello " + name + "!";
        }
    }
}
