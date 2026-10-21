// @@@SNIPSTART go-enrich-ui-static-summary
package main

import (
	"context"
	"log"

	"go.temporal.io/sdk/client"

	"my-org/quickstart/platform/enrichui"
)

func main() {
	// Create the client
	c, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	// Start workflow options with static summary and details
	workflowOptions := client.StartWorkflowOptions{
		ID:            "your-workflow-id",
		TaskQueue:     "your-task-queue",
		StaticSummary: "Order processing for customer #12345",
		StaticDetails: "Processing premium order with expedited shipping",
	}

	// Start the workflow
	we, err := c.ExecuteWorkflow(context.Background(), workflowOptions, enrichui.YourWorkflow, "workflow input")
	if err != nil {
		log.Fatalln("Unable to execute workflow", err)
	}
	log.Println("Started workflow", "WorkflowID", we.GetID(), "RunID", we.GetRunID())
}
// @@@SNIPEND
