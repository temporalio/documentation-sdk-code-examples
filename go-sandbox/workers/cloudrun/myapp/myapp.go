package myapp

import (
	"context"
	"time"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/workflow"
)

type MyInput struct {
	Items []string
}

func MyWorkflow(ctx workflow.Context, input MyInput) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Minute,
		HeartbeatTimeout:    30 * time.Second,
	})
	var result string
	err := workflow.ExecuteActivity(ctx, MyActivity, input).Get(ctx, &result)
	return result, err
}

// @@@SNIPSTART go-serverless-cloud-run-heartbeat
func MyActivity(ctx context.Context, input MyInput) (string, error) {
	// Resume after the last item a previous attempt recorded.
	start := 0
	if activity.HasHeartbeatDetails(ctx) {
		var lastIndex int
		if err := activity.GetHeartbeatDetails(ctx, &lastIndex); err == nil {
			start = lastIndex + 1
		}
	}
	for i := start; i < len(input.Items); i++ {
		// ... process input.Items[i]
		activity.RecordHeartbeat(ctx, i)
	}
	return "done", nil
}
// @@@SNIPEND
