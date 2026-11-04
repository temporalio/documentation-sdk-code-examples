// @@@SNIPSTART java-enriching-ui-current-details
package io.temporal.docs.platform.enrichingui;

import io.temporal.workflow.Workflow;

public class CurrentDetailsWorkflowImpl implements YourWorkflow {
    @Override
    public String yourWorkflowMethod(String input) {
        // Get the current details
        String currentDetails = Workflow.getCurrentDetails();
        Workflow.getLogger(CurrentDetailsWorkflowImpl.class).info("Current details: {}", currentDetails);

        // Set/update the current details
        Workflow.setCurrentDetails("Updated workflow details with new status");

        return "Workflow completed";
    }
}
// @@@SNIPEND
