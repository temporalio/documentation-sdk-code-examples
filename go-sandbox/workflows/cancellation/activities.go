package cancellation

import (
	"context"
	"sync/atomic"
	"time"

	"go.temporal.io/sdk/activity"
)

type Activities struct {
	// CleanupCalls counts CleanupActivity runs so tests can check that cleanup happened.
	CleanupCalls atomic.Int32
}

// @@@SNIPSTART go-cancellation-activity
func (a *Activities) ActivityToBeCanceled(ctx context.Context) (string, error) {
	logger := activity.GetLogger(ctx)
	logger.Info("Activity started, to cancel the Workflow Execution and this Activity, use 'go run ./workflows/cancellation/cancel' " +
		"or use the CLI: 'temporal workflow cancel --workflow-id <WorkflowID>'")
	// A for select statement is a common approach to listening for a Cancellation in an Activity
	for {
		select {
		case <-time.After(1 * time.Second):
			logger.Info("Heartbeating...")
			activity.RecordHeartbeat(ctx, "")
		// Listen for ctx.Done() to know if a Cancellation Request has propagated to the Activity.
		case <-ctx.Done():
			logger.Info("This Activity is canceled!")
			return "I am canceled by Done", nil
		}
	}
}
// @@@SNIPEND

func (a *Activities) ActivityToBeSkipped(ctx context.Context) error {
	activity.GetLogger(ctx).Info("This Activity runs only if the Workflow isn't canceled")
	return nil
}

func (a *Activities) CleanupActivity(ctx context.Context) error {
	activity.GetLogger(ctx).Info("Cleanup Activity started")
	a.CleanupCalls.Add(1)
	return nil
}
