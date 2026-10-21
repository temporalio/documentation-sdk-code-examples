// Package timeskip shows that the test environment skips Workflow Timers.
package timeskip

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// MyWorkflow sleeps for a day. In the test environment, the Timer fires without waiting.
func MyWorkflow(ctx workflow.Context) error {
	return workflow.Sleep(ctx, 24*time.Hour)
}
