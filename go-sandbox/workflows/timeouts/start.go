package timeouts

import (
	"context"
	"time"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/temporal"
)

// StartWithTimeout starts a Workflow Execution with a Workflow Execution Timeout.
func StartWithTimeout(temporalClient client.Client) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-workflow-timeouts-set
	workflowOptions := client.StartWorkflowOptions{
		ID:        "your-workflow-id",
		TaskQueue: TaskQueueName,
		// Set Workflow Timeout duration
		WorkflowExecutionTimeout: 24 * 365 * 10 * time.Hour,
		// WorkflowRunTimeout: 24 * 365 * 10 * time.Hour,
		// WorkflowTaskTimeout: 10 * time.Second,
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}

// StartWithRetryPolicy starts a Workflow Execution that retries on failure.
func StartWithRetryPolicy(temporalClient client.Client) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-workflow-retry-policy
	retryPolicy := &temporal.RetryPolicy{
		InitialInterval:    time.Second,
		BackoffCoefficient: 2.0,
		MaximumInterval:    time.Second * 100,
	}
	workflowOptions := client.StartWorkflowOptions{
		ID:          "your-workflow-id",
		TaskQueue:   TaskQueueName,
		RetryPolicy: retryPolicy,
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}
