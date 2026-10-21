package enrichui

// @@@SNIPSTART go-enrich-ui-current-details
import (
	"go.temporal.io/sdk/workflow"
)

func YourWorkflow(ctx workflow.Context, input string) (string, error) {
	// Get the current details
	currentDetails := workflow.GetCurrentDetails(ctx)
	workflow.GetLogger(ctx).Info("Current details", "details", currentDetails)

	// Set/update the current details
	workflow.SetCurrentDetails(ctx, "Updated workflow details with new status")

	return "Workflow completed", nil
}
// @@@SNIPEND
