// @@@SNIPSTART go-streams-chat-activity
package chat

import (
	"context"
	"strings"
	"time"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/contrib/workflowstreams"
)

type TextDelta struct {
	Text string
}

type RetryEvent struct {
	Attempt int32
}

func StreamCompletion(ctx context.Context, prompt string) (string, error) {
	attempt := activity.GetInfo(ctx).Attempt
	streamClient, err := workflowstreams.NewClientFromActivity(ctx, workflowstreams.Options{
		BatchInterval: 200 * time.Millisecond,
	})
	if err != nil {
		return "", err
	}

	deltas := streamClient.Topic("delta")
	retry := streamClient.Topic("retry")
	closeTopic := streamClient.Topic("close")

	// Tell consumers an earlier attempt's deltas are stale.
	if attempt > 1 {
		retry.Publish(RetryEvent{Attempt: attempt}, true)
	}

	var full []string
	first := true
	// generateDeltas wraps the model call and yields tokens as they arrive.
	// Disable provider-side retries; let Temporal own retry policy at the Activity layer.
	for token := range generateDeltas(prompt) {
		// forceFlush only on the first delta so the user sees something
		// immediately; subsequent deltas batch at the 200 ms interval.
		deltas.Publish(TextDelta{Text: token}, first)
		first = false
		full = append(full, token)
	}
	closeTopic.Publish(struct{}{}, false)
	// Close flushes the buffer. Return its error so a failed flush fails the Activity.
	if err := streamClient.Close(ctx); err != nil {
		return "", err
	}
	return strings.Join(full, ""), nil
}
// @@@SNIPEND
