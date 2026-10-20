package greeting

import (
	"context"
	"fmt"
	"time"

	"go.temporal.io/sdk/workflow"
)

func SampleWorkflow(ctx workflow.Context, name string) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
	})
	var result string
	err := workflow.ExecuteActivity(ctx, HelloActivity, name).Get(ctx, &result)
	return result, err
}

func HelloActivity(ctx context.Context, name string) (string, error) {
	return fmt.Sprintf("Hello, %s!", name), nil
}
