package versioning

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

const TaskQueue = "versioning"

var activityOptions = workflow.ActivityOptions{
	ScheduleToStartTimeout: time.Minute,
	StartToCloseTimeout:    time.Minute,
}

// @@@SNIPSTART go-versioning-original
func YourWorkflow(ctx workflow.Context, data string) (string, error) {
	ao := workflow.ActivityOptions{
		ScheduleToStartTimeout: time.Minute,
		StartToCloseTimeout:    time.Minute,
	}
	ctx = workflow.WithActivityOptions(ctx, ao)
	var result1 string
	err := workflow.ExecuteActivity(ctx, ActivityA, data).Get(ctx, &result1)
	if err != nil {
		return "", err
	}
	var result2 string
	err = workflow.ExecuteActivity(ctx, ActivityB, result1).Get(ctx, &result2)
	return result2, err
}
// @@@SNIPEND
