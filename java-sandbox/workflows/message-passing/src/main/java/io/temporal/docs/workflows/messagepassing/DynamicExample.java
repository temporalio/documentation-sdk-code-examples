package io.temporal.docs.workflows.messagepassing;

import io.temporal.activity.Activity;
import io.temporal.activity.DynamicActivity;
import io.temporal.common.converter.EncodedValues;
import io.temporal.workflow.DynamicQueryHandler;
import io.temporal.workflow.DynamicSignalHandler;
import io.temporal.workflow.DynamicUpdateHandler;
import io.temporal.workflow.DynamicWorkflow;
import io.temporal.workflow.Workflow;

public class DynamicExample {

    // @@@SNIPSTART java-message-passing-dynamic-workflow
    public static class MyDynamicWorkflow implements DynamicWorkflow {
        @Override
        public Object execute(EncodedValues args) {
            String workflowType = Workflow.getInfo().getWorkflowType();
            return workflowType + ": " + args.get(0, String.class);
        }
    }
    // @@@SNIPEND

    // @@@SNIPSTART java-message-passing-dynamic-activity
    // Dynamic Activity implementation
    public static class DynamicGreetingActivityImpl implements DynamicActivity {
        @Override
        public Object execute(EncodedValues args) {
            String activityType = Activity.getExecutionContext().getInfo().getActivityType();
            return activityType
                + ": "
                + args.get(0, String.class)
                + " "
                + args.get(1, String.class)
                + " from: "
                + args.get(2, String.class);
        }
    }
    // @@@SNIPEND

    public static class DynamicHandlersWorkflow implements DynamicWorkflow {
        private String name = "";

        @Override
        public Object execute(EncodedValues args) {
            // @@@SNIPSTART java-message-passing-dynamic-signal
            Workflow.registerListener(
                (DynamicSignalHandler)
                    (signalName, encodedArgs) -> name = encodedArgs.get(0, String.class));
            // @@@SNIPEND

            // @@@SNIPSTART java-message-passing-dynamic-query
            Workflow.registerListener(
                (DynamicQueryHandler)
                    (queryName, encodedArgs) -> name);
            // @@@SNIPEND

            // @@@SNIPSTART java-message-passing-dynamic-update
            Workflow.registerListener(
                (DynamicUpdateHandler)
                    (updateName, encodedArgs) -> encodedArgs.get(0, String.class));
            // @@@SNIPEND

            Workflow.await(() -> "done".equals(name));
            return name;
        }
    }
}
