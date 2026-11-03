package io.temporal.docs.workflows.basics;

import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import org.slf4j.Logger;

public class LoggingExample {

    @WorkflowInterface
    public interface MyWorkflow {
        @WorkflowMethod
        String execute(String name);
    }

    // @@@SNIPSTART java-workflow-basics-logging
    public static class MyWorkflowImpl implements MyWorkflow {
      private static final Logger logger = Workflow.getLogger(MyWorkflowImpl.class);

      @Override
      public String execute(String name) {
        logger.info("Starting workflow for {}", name);
        return "Hello " + name;
      }
    }
    // @@@SNIPEND
}
