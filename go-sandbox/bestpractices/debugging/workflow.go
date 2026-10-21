package debugging

import (
	"context"
	"time"

	"go.temporal.io/sdk/temporal"
	"go.temporal.io/sdk/workflow"
)

// SimpleWorkflow runs SimpleActivity once and returns its error, if any.
func SimpleWorkflow(ctx workflow.Context, value string) error {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
		// Fail on the first error so a failed Activity fails the Workflow.
		RetryPolicy: &temporal.RetryPolicy{MaximumAttempts: 1},
	})
	var result string
	return workflow.ExecuteActivity(ctx, SimpleActivity, value).Get(ctx, &result)
}

func SimpleActivity(ctx context.Context, value string) (string, error) {
	return value, nil
}

// @@@SNIPSTART go-debugging-progress-workflow
func ProgressWorkflow(ctx workflow.Context, percent int) error {
	logger := workflow.GetLogger(ctx)

	err := workflow.SetQueryHandler(ctx, "getProgress", func(input []byte) (int, error) {
		return percent, nil
	})
	if err != nil {
		logger.Info("SetQueryHandler failed.", "Error", err)
		return err
	}

	for percent = 0; percent < 100; percent++ {
		// Important! Use `workflow.Sleep()`, not `time.Sleep()`, because Temporal's
		// test environment doesn't stub out `time.Sleep()`.
		if err := workflow.Sleep(ctx, time.Second*1); err != nil {
			return err
		}
	}

	return nil
}
// @@@SNIPEND
