package io.temporal.docs.platform.observability.logging;

import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

public class GreetingWorkflowImpl implements GreetingWorkflow {

    // @@@SNIPSTART java-observability-workflow-logger
    private static final Logger logger = Workflow.getLogger(GreetingWorkflowImpl.class);
    // @@@SNIPEND

    @Override
    public String greet(String name) {
        logger.info("Greeting {}", name);
        return "Hello, " + name + "!";
    }
}
