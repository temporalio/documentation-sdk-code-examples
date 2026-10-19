package cancellation

import (
	"errors"
	"fmt"
	"time"

	"go.temporal.io/sdk/workflow"
)

// @@@SNIPSTART go-cancellation-workflow
const WorkflowID = "example-cancellation-workflow"
const TaskQueueName = "cancellation"

func YourWorkflow(ctx workflow.Context) error {
	logger := workflow.GetLogger(ctx)
	var a *Activities
	activityOptions := workflow.ActivityOptions{
		StartToCloseTimeout: 30 * time.Minute,
		HeartbeatTimeout:    5 * time.Second,
		WaitForCancellation: true,
	}
	defer func() {
		if !errors.Is(ctx.Err(), workflow.ErrCanceled) {
			return
		}

		// A disconnected Context isn't canceled, so the cleanup Activity can still run.
		newCtx, _ := workflow.NewDisconnectedContext(ctx)

		err := workflow.ExecuteActivity(newCtx, a.CleanupActivity).Get(newCtx, nil)
		if err != nil {
			logger.Error("CleanupActivity failed", "Error", err)
		}
	}()

	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	var result string

	err := workflow.ExecuteActivity(ctx, a.ActivityToBeCanceled).Get(ctx, &result)
	logger.Info(fmt.Sprintf("ActivityToBeCanceled returns %v, %v", result, err))

	err = workflow.ExecuteActivity(ctx, a.ActivityToBeSkipped).Get(ctx, nil)
	logger.Error("Error from ActivityToBeSkipped", "Error", err)

	return err
}
// @@@SNIPEND
