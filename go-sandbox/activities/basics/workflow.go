// @@@SNIPSTART go-activities-execution-workflow
package basics

import (
	"time"

	"go.temporal.io/sdk/temporal"
	"go.temporal.io/sdk/workflow"
)

type YourWorkflowParam struct {
	WorkflowParamX string
	WorkflowParamY int
}

type YourWorkflowResultObject struct {
	WFResultFieldX string
	WFResultFieldY int
}

func YourWorkflowDefinition(ctx workflow.Context, param YourWorkflowParam) (*YourWorkflowResultObject, error) {
	retryPolicy := &temporal.RetryPolicy{
		InitialInterval:        time.Second,
		BackoffCoefficient:     2.0,
		MaximumInterval:        time.Second * 100, // 100 * InitialInterval
		MaximumAttempts:        0,                 // Unlimited
		NonRetryableErrorTypes: []string{},        // empty
	}
	activityOptions := workflow.ActivityOptions{
		ActivityID:          "your-activity-id",
		TaskQueue:           "your-custom-task-queue-name",
		StartToCloseTimeout: 10 * time.Second,
		HeartbeatTimeout:    5 * time.Second,
		WaitForCancellation: false,
		RetryPolicy:         retryPolicy,
		// ScheduleToCloseTimeout: 10 * time.Second,
		// ScheduleToStartTimeout: 10 * time.Second,
	}
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	activityParam := YourActivityParam{
		ActivityParamX: param.WorkflowParamX,
		ActivityParamY: param.WorkflowParamY,
	}

	var a *YourActivityObject

	var activityResult YourActivityResultObject

	future := workflow.ExecuteActivity(ctx, a.YourActivityDefinition, activityParam)
	if future.IsReady() {
		var yourActivityResult YourActivityResultObject
		if err := future.Get(ctx, &yourActivityResult); err != nil {
			return nil, err
		}
	}

	// This approach is fine if you don't use isReady() to check the future before getting the result
	if err := future.Get(ctx, &activityResult); err != nil {
		return nil, err
	}

	workflowResult := &YourWorkflowResultObject{
		WFResultFieldX: activityResult.ResultFieldX,
		WFResultFieldY: activityResult.ResultFieldY,
	}
	return workflowResult, nil
}

// @@@SNIPEND

// @@@SNIPSTART go-workflows-basics-simple
func YourSimpleWorkflowDefinition(ctx workflow.Context) error {
	// ...
	return nil
}
// @@@SNIPEND
