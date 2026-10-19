package schedules

import (
	"context"
	"time"

	"go.temporal.io/sdk/client"
)

// StartWithDelay starts a Workflow Execution after a one-time delay.
func StartWithDelay(temporalClient client.Client) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-schedule-start-delay
	workflowOptions := client.StartWorkflowOptions{
		ID:        "your-workflow-id",
		TaskQueue: TaskQueue,
		// Start the workflow in 12 hours
		StartDelay: 12 * time.Hour,
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}

// StartCronWorkflow starts a Temporal Cron Job.
func StartCronWorkflow(temporalClient client.Client) (client.WorkflowRun, error) {
	// @@@SNIPSTART go-schedule-cron
	workflowOptions := client.StartWorkflowOptions{
		ID:           "your-cron-workflow-id",
		TaskQueue:    TaskQueue,
		CronSchedule: "15 8 * * *",
	}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return workflowRun, nil
}
