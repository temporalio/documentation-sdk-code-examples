package schedules

import "go.temporal.io/sdk/workflow"

const TaskQueue = "schedule"

func ScheduleWorkflow(ctx workflow.Context) error {
	workflow.GetLogger(ctx).Info("Schedule Workflow started", "WorkflowID", workflow.GetInfo(ctx).WorkflowExecution.ID)
	return nil
}

func YourWorkflowDefinition(ctx workflow.Context) error {
	return nil
}
