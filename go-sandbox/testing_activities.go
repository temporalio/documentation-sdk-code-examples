package quickstart

import (
	"context"
	"fmt"
	"time"

	"go.temporal.io/sdk/activity"
)

// GreetWithHeartbeat sends a Heartbeat before it returns the greeting.
func GreetWithHeartbeat(ctx context.Context, name string) (string, error) {
	activity.RecordHeartbeat(ctx, "status-report-to-workflow")
	return fmt.Sprintf("Hello %s", name), nil
}

// LongRunningGreet Heartbeats until it's canceled, then returns the cancellation error.
func LongRunningGreet(ctx context.Context, name string) (string, error) {
	for {
		select {
		case <-ctx.Done():
			return "", ctx.Err()
		case <-time.After(time.Second):
			activity.RecordHeartbeat(ctx)
		}
	}
}
