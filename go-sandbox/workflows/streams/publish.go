package streams

import (
	"context"
	"time"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/workflowstreams"
)

// NewStreamClient returns a Workflow Streams client for publishing to or subscribing to a Workflow.
func NewStreamClient(temporalClient client.Client, workflowID string) *workflowstreams.Client {
	// @@@SNIPSTART go-streams-new-client
	streamClient := workflowstreams.NewClient(temporalClient, workflowID, workflowstreams.Options{})
	// @@@SNIPEND
	return streamClient
}

// NewRetryingStreamClient returns a client that retries failed batches for up to 10 minutes.
func NewRetryingStreamClient(temporalClient client.Client, workflowID string) *workflowstreams.Client {
	// @@@SNIPSTART go-streams-max-retry-duration
	streamClient := workflowstreams.NewClient(temporalClient, workflowID, workflowstreams.Options{
		MaxRetryDuration: 10 * time.Minute,
	})
	// @@@SNIPEND
	return streamClient
}

// @@@SNIPSTART go-streams-publish-from-client
func PublishStatus(ctx context.Context, temporalClient client.Client, workflowID string) error {
	streamClient := workflowstreams.NewClient(temporalClient, workflowID, workflowstreams.Options{
		BatchInterval: 200 * time.Millisecond,
	})

	status := streamClient.Topic("status")
	status.Publish(StatusEvent{State: "started"}, false)
	// ...
	// Close flushes the buffer. Check its error to know the events were delivered.
	return streamClient.Close(ctx)
}
// @@@SNIPEND

// PublishUrgent ships a delta without waiting for the next batch interval.
func PublishUrgent(deltas *workflowstreams.TopicHandle, delta Delta) {
	// @@@SNIPSTART go-streams-force-flush
	deltas.Publish(delta, true)
	// @@@SNIPEND
}

// PublishInPhases publishes two phases of events with a durability barrier between them.
func PublishInPhases(ctx context.Context, temporalClient client.Client, workflowID string) (err error) {
	// @@@SNIPSTART go-streams-flush-barrier
	streamClient := workflowstreams.NewClient(temporalClient, workflowID, workflowstreams.Options{})
	defer func() {
		if closeErr := streamClient.Close(ctx); err == nil {
			err = closeErr
		}
	}()
	deltas := streamClient.Topic("delta")

	for _, delta := range firstPhase() {
		deltas.Publish(delta, false)
	}

	if err := streamClient.Flush(ctx); err != nil {
		return err
	}
	checkpointID, err := recordPhaseOneComplete(ctx) // only safe once phase-one events are durable
	if err != nil {
		return err
	}

	for _, delta := range secondPhase(checkpointID) {
		deltas.Publish(delta, false)
	}
	// @@@SNIPEND
	return nil
}
