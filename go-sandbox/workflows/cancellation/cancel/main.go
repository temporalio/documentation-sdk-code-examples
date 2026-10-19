package main

import (
	"context"
	"log"

	"go.temporal.io/sdk/client"

	"my-org/quickstart/workflows/cancellation"
)

// @@@SNIPSTART go-cancellation-request
func main() {
	temporalClient, err := client.Dial(client.Options{
		HostPort: client.DefaultHostPort,
	})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()
	// Call the CancelWorkflow API to cancel a Workflow
	// In this call we are relying on the Workflow ID only.
	// But a Run ID can also be supplied to ensure the correct Workflow is Canceled.
	err = temporalClient.CancelWorkflow(context.Background(), cancellation.WorkflowID, "")
	if err != nil {
		log.Fatalln("Unable to cancel Workflow Execution", err)
	}
	log.Println("Workflow Execution cancelled", "WorkflowID", cancellation.WorkflowID)
}
// @@@SNIPEND
