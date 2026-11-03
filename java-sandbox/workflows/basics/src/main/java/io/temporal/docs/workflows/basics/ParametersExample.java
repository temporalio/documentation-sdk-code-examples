package io.temporal.docs.workflows.basics;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class ParametersExample {

    public static class CustomObj {
        public String name;
        public int count;

        public CustomObj() {}

        public CustomObj(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    // @@@SNIPSTART java-workflow-basics-parameters
    @WorkflowInterface
    public interface YourWorkflow {
        @WorkflowMethod
        String yourWFMethod(CustomObj customobj);
    }
    // @@@SNIPEND

    public static class YourWorkflowImpl implements YourWorkflow {
        @Override
        public String yourWFMethod(CustomObj customobj) {
            return customobj.name + " x" + customobj.count;
        }
    }
}
