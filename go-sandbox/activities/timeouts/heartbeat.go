package timeouts

import (
	"context"

	"go.temporal.io/sdk/client"
)

// RecordExternalHeartbeat Heartbeats an Activity from outside the Worker, using
// the Task Token retrieved inside the Activity.
func RecordExternalHeartbeat(ctx context.Context, taskToken []byte, details string) error {
	// @@@SNIPSTART go-activity-heartbeat-external
	// The client is a heavyweight object that should be created once per process.
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		return err
	}
	defer temporalClient.Close()

	// Record heartbeat.
	return temporalClient.RecordActivityHeartbeat(ctx, taskToken, details)
	// @@@SNIPEND
}
