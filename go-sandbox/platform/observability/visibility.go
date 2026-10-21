package observability

import (
	"context"
	"fmt"

	"go.temporal.io/api/workflowservice/v1"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/workflow"
)

// ListOpenWorkflows prints the Workflow IDs in the first page of open Workflow Executions.
func ListOpenWorkflows(temporalClient client.Client) error {
	// @@@SNIPSTART go-observability-list-request
	request := &workflowservice.ListWorkflowExecutionsRequest{Query: "ExecutionStatus = 'Running'"}
	// @@@SNIPEND

	// @@@SNIPSTART go-observability-list-workflows
	resp, err := temporalClient.ListWorkflow(context.Background(), request)
	if err != nil {
		return err
	}

	fmt.Println("First page of results:")
	for _, exec := range resp.Executions {
		fmt.Printf("Workflow ID %v\n", exec.Execution.WorkflowId)
	}
	// @@@SNIPEND
	return nil
}

// Client wraps a Temporal Client for an application.
type Client struct {
	Client client.Client
}

func YourWorkflow(ctx workflow.Context, payload map[string]interface{}) error {
	return nil
}

// @@@SNIPSTART go-observability-set-search-attributes
func (c *Client) CallYourWorkflow(ctx context.Context, workflowID string, payload map[string]interface{}) error {
	// ...
	searchAttributes := map[string]interface{}{
		"CustomerId": payload["customer"],
		"MiscData":   payload["miscData"],
	}
	options := client.StartWorkflowOptions{
		ID:               workflowID,
		TaskQueue:        "your-task-queue",
		SearchAttributes: searchAttributes,
		// ...
	}
	_, err := c.Client.ExecuteWorkflow(ctx, options, YourWorkflow, payload)
	// ...
	return err
}
// @@@SNIPEND

// @@@SNIPSTART go-observability-upsert-search-attributes
func UpsertWorkflow(ctx workflow.Context, input string) error {

	attr1 := map[string]interface{}{
		"CustomIntField":  1,
		"CustomBoolField": true,
	}
	if err := workflow.UpsertSearchAttributes(ctx, attr1); err != nil {
		return err
	}

	attr2 := map[string]interface{}{
		"CustomIntField":     2,
		"CustomKeywordField": "seattle",
	}
	if err := workflow.UpsertSearchAttributes(ctx, attr2); err != nil {
		return err
	}
	return nil
}
// @@@SNIPEND
