package dynamic

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

const TaskQueue = "dynamic-activity"

// CallUnregisteredActivityWorkflow calls an Activity Type that no Activity is
// registered under, so the Worker runs the Dynamic Activity instead.
func CallUnregisteredActivityWorkflow(ctx workflow.Context, arg1, arg2 string) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
	})

	var result string
	err := workflow.ExecuteActivity(ctx, "UnregisteredActivityType", arg1, arg2).Get(ctx, &result)
	return result, err
}
