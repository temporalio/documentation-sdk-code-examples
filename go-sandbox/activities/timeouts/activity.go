package timeouts

import (
	"context"
	"fmt"
	"time"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/temporal"
)

type YourActivityParam struct {
	Items []string
}

type YourActivityResult struct {
	ProcessedCount int
}

// @@@SNIPSTART go-activity-heartbeat
func YourActivityDefinition(ctx context.Context, param YourActivityParam) (YourActivityResult, error) {
	for i := range param.Items {
		// Process item i...
		details := fmt.Sprintf("processed item %d", i)
		activity.RecordHeartbeat(ctx, details)
	}
	return YourActivityResult{ProcessedCount: len(param.Items)}, nil
}
// @@@SNIPEND

func NextRetryDelayActivity(ctx context.Context) error {
	// @@@SNIPSTART go-activity-next-retry-delay
	attempt := activity.GetInfo(ctx).Attempt

	return temporal.NewApplicationErrorWithOptions(fmt.Sprintf("Something bad happened on attempt %d", attempt), "NextDelay", temporal.ApplicationErrorOptions{
		NextRetryDelay: 3 * time.Second * time.Duration(attempt),
	})
	// @@@SNIPEND
}

type InputParams struct {
	StartIndex int
	EndIndex   int
}

// @@@SNIPSTART go-activity-heartbeat-resume
func SampleActivity(ctx context.Context, inputArg InputParams) error {
	startIdx := inputArg.StartIndex
	if activity.HasHeartbeatDetails(ctx) {
		// Recover from finished progress.
		var finishedIndex int
		if err := activity.GetHeartbeatDetails(ctx, &finishedIndex); err == nil {
			startIdx = finishedIndex + 1 // Start from next one.
		}
	}

	// Normal Activity logic...
	for i := startIdx; i < inputArg.EndIndex; i++ {
		// Code for processing item i goes here...
		activity.RecordHeartbeat(ctx, i) // Report progress.
	}
	return nil
}
// @@@SNIPEND
