// @@@SNIPSTART go-streams-chat-consumer
package chat

import (
	"context"
	"strings"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/workflowstreams"
	"go.temporal.io/sdk/converter"
)

func StreamChat(ctx context.Context, temporalClient client.Client, chatID string) (string, error) {
	// Subscribe-only; no Close needed because the flusher only runs for publishers.
	stream := workflowstreams.NewClient(temporalClient, chatID, workflowstreams.Options{})
	dc := converter.GetDefaultDataConverter()
	var output []string

	render := func() {
		// ... display the accumulated output (terminal redraw, UI update, etc.)
	}

	for item, err := range stream.Subscribe(ctx, workflowstreams.SubscribeOptions{Topics: []string{"delta", "retry", "close"}}) {
		if err != nil {
			return "", err
		}
		switch item.Topic {
		case "retry":
			// Earlier attempt's deltas are stale; drop what we've shown.
			output = output[:0]
			render()
		case "delta":
			var delta TextDelta
			if err := dc.FromPayload(item.Data, &delta); err != nil {
				return "", err
			}
			output = append(output, delta.Text)
			render()
		case "close":
			// Acknowledge so the Workflow can return without waiting on the fallback timeout.
			if err := temporalClient.SignalWorkflow(ctx, chatID, "", "subscriber-acknowledged-terminator", nil); err != nil {
				return "", err
			}
			return strings.Join(output, ""), nil
		}
	}

	return strings.Join(output, ""), nil
}
// @@@SNIPEND
