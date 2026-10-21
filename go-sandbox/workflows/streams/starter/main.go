package main

import (
	"context"
	"log"
	"time"

	"go.temporal.io/sdk/client"

	"my-org/quickstart/workflows/streams"
	"my-org/quickstart/workflows/streams/chat"
)

// main runs the order and chat examples against a running Worker.
func main() {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	// Order: the Workflow publishes status events. A client also publishes to the same stream.
	orderID := "order-" + time.Now().Format("150405")
	if _, err := temporalClient.ExecuteWorkflow(ctx, client.StartWorkflowOptions{ID: orderID, TaskQueue: streams.TaskQueue},
		streams.OrderWorkflow, streams.OrderInput{OrderID: orderID}); err != nil {
		log.Fatalln("Unable to start OrderWorkflow", err)
	}
	if err := streams.PublishStatus(ctx, temporalClient, orderID); err != nil {
		log.Fatalln("PublishStatus failed", err)
	}
	if err := streams.PublishInPhases(ctx, temporalClient, orderID); err != nil {
		log.Fatalln("PublishInPhases failed", err)
	}
	if err := streams.WatchOrder(ctx, temporalClient, orderID); err != nil {
		log.Fatalln("WatchOrder failed", err)
	}

	// Chat: an Activity publishes deltas, and the consumer acknowledges the terminator.
	chatID := "chat-" + time.Now().Format("150405")
	run, err := temporalClient.ExecuteWorkflow(ctx, client.StartWorkflowOptions{ID: chatID, TaskQueue: streams.TaskQueue},
		chat.ChatWorkflow, chat.ChatInput{Prompt: "Where is my order?"})
	if err != nil {
		log.Fatalln("Unable to start ChatWorkflow", err)
	}
	output, err := chat.StreamChat(ctx, temporalClient, chatID)
	if err != nil {
		log.Fatalln("StreamChat failed", err)
	}
	log.Println("Streamed chat output:", output)

	start := time.Now()
	var result string
	if err := run.Get(ctx, &result); err != nil {
		log.Fatalln("ChatWorkflow failed", err)
	}
	log.Printf("ChatWorkflow returned %q %s after the ack", result, time.Since(start).Round(time.Millisecond))
}
