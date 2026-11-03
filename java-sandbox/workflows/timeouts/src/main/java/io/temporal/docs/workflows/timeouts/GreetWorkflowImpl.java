package io.temporal.docs.workflows.timeouts;

import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class GreetWorkflowImpl implements GreetWorkflowInterface {

    @Override
    public String greet(String name) {
        // "slow" outlasts a short Workflow Execution Timeout.
        if (name.equals("slow")) {
            Workflow.sleep(Duration.ofMinutes(1));
        }
        // "flaky" fails until the third attempt, so a Workflow Retry Policy can recover it.
        if (name.equals("flaky") && Workflow.getInfo().getAttempt() < 3) {
            throw ApplicationFailure.newFailure("Attempt " + Workflow.getInfo().getAttempt() + " failed", "Flaky");
        }
        return "Hello " + name + "!";
    }
}
