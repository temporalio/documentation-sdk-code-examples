package streams

// @@@SNIPSTART go-streams-publish-from-activity
import (
	"context"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/contrib/workflowstreams"
)

func StreamDeltas(ctx context.Context, orderID string) error {
	streamClient, err := workflowstreams.NewClientFromActivity(ctx, workflowstreams.Options{})
	if err != nil {
		return err
	}

	deltas := streamClient.Topic("delta")
	for delta := range generateDeltas(orderID) {
		deltas.Publish(delta, false)
		activity.RecordHeartbeat(ctx)
	}
	// Close flushes the buffer. Return its error so a failed flush fails the Activity.
	return streamClient.Close(ctx)
}
// @@@SNIPEND
