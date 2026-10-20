package options

import (
	"context"
	"time"

	enums "go.temporal.io/api/enums/v1"
	"go.temporal.io/sdk/client"
)

// The functions below each set one StartWorkflowOptions field.
// Every Workflow Execution also needs a TaskQueue, so each sets one.
const taskQueue = "your-task-queue"

func StartByFunction(temporalClient client.Client, workflowOptions client.StartWorkflowOptions, param YourWorkflowParam) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-client-start-by-function
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	// @@@SNIPEND
	return workflowRun, err
}

func StartByName(temporalClient client.Client, workflowOptions client.StartWorkflowOptions, param YourWorkflowParam) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-client-start-by-name
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, "YourWorkflowDefinition", param)
	// @@@SNIPEND
	return workflowRun, err
}

func StartWithTaskQueue(temporalClient client.Client, param YourWorkflowParam) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-client-option-task-queue
	workflowOptions := client.StartWorkflowOptions{
		// ...
		TaskQueue: "your-task-queue",
		// ...
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}

func StartWithID(temporalClient client.Client, param YourWorkflowParam) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-client-option-id
	workflowOptions := client.StartWorkflowOptions{
		// ...
		ID:        "Your-Custom-Workflow-Id",
		TaskQueue: taskQueue,
		// ...
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}

func StartWithRunTimeout(temporalClient client.Client, param YourWorkflowParam) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-client-option-run-timeout
	workflowOptions := client.StartWorkflowOptions{
		WorkflowRunTimeout: 24 * 365 * 10 * time.Hour,
		TaskQueue:          taskQueue,
		// ...
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}

func StartWithTaskTimeout(temporalClient client.Client, param YourWorkflowParam) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-client-option-task-timeout
	workflowOptions := client.StartWorkflowOptions{
		WorkflowTaskTimeout: 10 * time.Second,
		TaskQueue:           taskQueue,
		// ...
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}

func StartWithIDReusePolicy(temporalClient client.Client, param YourWorkflowParam) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-client-option-id-reuse-policy
	workflowOptions := client.StartWorkflowOptions{
		WorkflowIDReusePolicy: enums.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE,
		TaskQueue:             taskQueue,
		// ...
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}

func StartWithErrorWhenAlreadyStarted(temporalClient client.Client, param YourWorkflowParam) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-client-option-error-when-already-started
	workflowOptions := client.StartWorkflowOptions{
		WorkflowExecutionErrorWhenAlreadyStarted: false,
		TaskQueue:                                taskQueue,
		// ...
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}

func StartWithMemo(temporalClient client.Client, param YourWorkflowParam) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-client-option-memo
	workflowOptions := client.StartWorkflowOptions{
		Memo: map[string]interface{}{
			"description": "Test search attributes workflow",
		},
		TaskQueue: taskQueue,
		// ...
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}

func StartWithSearchAttributes(temporalClient client.Client, param YourWorkflowParam) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-client-option-search-attributes
	searchAttributes := map[string]interface{}{
		"CustomIntField": 1,
		"MiscData":       "yellow",
	}
	workflowOptions := client.StartWorkflowOptions{
		SearchAttributes: searchAttributes,
		TaskQueue:        taskQueue,
		// ...
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}
