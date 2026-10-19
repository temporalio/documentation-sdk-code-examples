package main

import (
	"context"
	"log"

	"go.temporal.io/sdk/client"

	"my-org/quickstart/workflows/cancellation"
)

func main() {
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	run, err := temporalClient.ExecuteWorkflow(context.Background(), client.StartWorkflowOptions{
		ID:        cancellation.WorkflowID,
		TaskQueue: cancellation.TaskQueueName,
	}, cancellation.YourWorkflow)
	if err != nil {
		log.Fatalln("Unable to execute Workflow", err)
	}
	log.Println("Started Workflow", "WorkflowID", run.GetID(), "RunID", run.GetRunID())
}
