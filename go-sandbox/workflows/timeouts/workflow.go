package timeouts

import "go.temporal.io/sdk/workflow"

const TaskQueueName = "workflow-timeouts"

func YourWorkflowDefinition(ctx workflow.Context) error {
	return nil
}
