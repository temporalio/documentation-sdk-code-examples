// @@@SNIPSTART go-streams-subscribe
package streams

import (
	"context"
	"fmt"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/workflowstreams"
	"go.temporal.io/sdk/converter"
)

func WatchOrder(ctx context.Context, temporalClient client.Client, orderID string) error {
	stream := workflowstreams.NewClient(temporalClient, orderID, workflowstreams.Options{})

	for item, err := range stream.Subscribe(ctx, workflowstreams.SubscribeOptions{Topics: []string{"status"}}) {
		if err != nil {
			return err
		}
		var evt StatusEvent
		if err := converter.GetDefaultDataConverter().FromPayload(item.Data, &evt); err != nil {
			return err
		}
		fmt.Printf("[%3d%%] %s: %s\n", evt.Progress, evt.State, evt.Detail)
		if evt.State == "completed" {
			break
		}
	}
	return nil
}
// @@@SNIPEND

// WatchOrderEvents prints status and progress events until the order completes.
func WatchOrderEvents(ctx context.Context, temporalClient client.Client, orderID string) error {
	stream := workflowstreams.NewClient(temporalClient, orderID, workflowstreams.Options{})

	// @@@SNIPSTART go-streams-heterogeneous-topics
	for item, err := range stream.Subscribe(ctx, workflowstreams.SubscribeOptions{Topics: []string{"status", "progress"}}) {
		if err != nil {
			return err
		}
		switch item.Topic {
		case "status":
			var evt StatusEvent
			if err := converter.GetDefaultDataConverter().FromPayload(item.Data, &evt); err != nil {
				return err
			}
			fmt.Printf("[status] %s: %s\n", evt.State, evt.Detail)
			if evt.State == "completed" {
				return nil
			}
		case "progress":
			var evt ProgressEvent
			if err := converter.GetDefaultDataConverter().FromPayload(item.Data, &evt); err != nil {
				return err
			}
			fmt.Printf("[progress] %s\n", evt.Message)
		}
	}
	// @@@SNIPEND
	return nil
}
