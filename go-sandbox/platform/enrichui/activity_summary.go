package enrichui

// @@@SNIPSTART go-enrich-ui-activity-summary
import (
	"time"

	"go.temporal.io/sdk/workflow"
)

func ActivitySummaryWorkflow(ctx workflow.Context, input string) (string, error) {
	// Activity options with summary
	ao := workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
		Summary:             "Processing user data",
	}
	ctx = workflow.WithActivityOptions(ctx, ao)

	// Execute the activity
	var result string
	err := workflow.ExecuteActivity(ctx, YourActivity, input).Get(ctx, &result)
	if err != nil {
		return "", err
	}

	return result, nil
}
// @@@SNIPEND
