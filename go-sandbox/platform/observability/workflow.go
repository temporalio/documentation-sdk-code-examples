package observability

// @@@SNIPSTART go-observability-workflow-logger
import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// Workflow is a standard workflow definition.
func Workflow(ctx workflow.Context, name string) (string, error) {
	ao := workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
	}
	ctx = workflow.WithActivityOptions(ctx, ao)

	// Getting the logger from the context.
	logger := workflow.GetLogger(ctx)
	// Logging a message with the key value pair `name` and `name`
	logger.Info("Workflow started", "name", name)

	var result string
	err := workflow.ExecuteActivity(ctx, Activity, name).Get(ctx, &result)
	if err != nil {
		logger.Error("Activity failed.", "Error", err)
		return "", err
	}

	logger.Info("Workflow completed.", "result", result)

	return result, nil
}
// @@@SNIPEND
