package options

import (
	"context"

	"go.temporal.io/sdk/client"
)

// StartAndWait starts a Workflow Execution and waits for its result in the same process.
func StartAndWait(temporalClient client.Client, workflowOptions client.StartWorkflowOptions, param YourWorkflowParam) (YourWorkflowResponse, error) {
	// @@@SNIPSTART go-client-get-result
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	if err != nil {
		return YourWorkflowResponse{}, err
	}
	var result YourWorkflowResponse
	err = workflowRun.Get(context.Background(), &result)
	if err != nil {
		return YourWorkflowResponse{}, err
	}
	// @@@SNIPEND
	return result, nil
}

// GetResultByID gets the result of a Workflow Execution started by another process.
func GetResultByID(temporalClient client.Client) (YourWorkflowResponse, error) {
	// @@@SNIPSTART go-client-get-workflow
	workflowID := "Your-Custom-Workflow-Id"
	// An empty Run ID gets the latest Run of the Workflow Execution.
	workflowRun := temporalClient.GetWorkflow(context.Background(), workflowID, "")

	var result YourWorkflowResponse
	err := workflowRun.Get(context.Background(), &result)
	if err != nil {
		return YourWorkflowResponse{}, err
	}
	// @@@SNIPEND
	return result, nil
}
