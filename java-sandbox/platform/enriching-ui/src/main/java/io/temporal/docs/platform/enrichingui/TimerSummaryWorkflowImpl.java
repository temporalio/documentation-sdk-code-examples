// @@@SNIPSTART java-enriching-ui-timer-summary
package io.temporal.docs.platform.enrichingui;

import io.temporal.workflow.TimerOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class TimerSummaryWorkflowImpl implements YourWorkflow {
    @Override
    public String yourWorkflowMethod(String input) {
        // Create a timer with a summary
        Workflow.newTimer(Duration.ofMinutes(5),
            TimerOptions.newBuilder()
                .setSummary("Waiting for payment confirmation")
                .build())
            .get(); // Wait for the timer to fire

        return "Timer completed";
    }
}
// @@@SNIPEND
