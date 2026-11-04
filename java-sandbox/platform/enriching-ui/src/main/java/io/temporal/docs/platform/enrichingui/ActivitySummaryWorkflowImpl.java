// @@@SNIPSTART java-enriching-ui-activity-summary
package io.temporal.docs.platform.enrichingui;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ActivitySummaryWorkflowImpl implements YourWorkflow {
    private final YourActivities activities =
        Workflow.newActivityStub(YourActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setSummary("Processing user data")
                .build());

    @Override
    public String yourWorkflowMethod(String input) {
        // Execute the activity with the summary
        String result = activities.yourActivity(input);
        return result;
    }
}
// @@@SNIPEND
