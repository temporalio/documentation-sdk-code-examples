package timeouts

import (
	"time"

	"go.temporal.io/sdk/temporal"
	"go.temporal.io/sdk/workflow"
)

func ActivityTimeoutWorkflow(ctx workflow.Context, yourActivityParam YourActivityParam) (*YourActivityResult, error) {
	// @@@SNIPSTART go-activity-timeouts-set
	activityOptions := workflow.ActivityOptions{
		// Set Activity Timeout duration
		ScheduleToCloseTimeout: 10 * time.Second,
		// StartToCloseTimeout: 10 * time.Second,
		// ScheduleToStartTimeout: 10 * time.Second,
	}
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	var yourActivityResult YourActivityResult
	err := workflow.ExecuteActivity(ctx, YourActivityDefinition, yourActivityParam).Get(ctx, &yourActivityResult)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return &yourActivityResult, nil
}

func CustomRetryPolicyWorkflow(ctx workflow.Context, yourActivityParam YourActivityParam) (*YourActivityResult, error) {
	// @@@SNIPSTART go-activity-retry-policy-custom
	retryPolicy := &temporal.RetryPolicy{
		InitialInterval:    time.Second,
		BackoffCoefficient: 2.0,
		MaximumInterval:    time.Second * 100,
	}

	activityOptions := workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
		RetryPolicy:         retryPolicy,
	}
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	var yourActivityResult YourActivityResult
	err := workflow.ExecuteActivity(ctx, YourActivityDefinition, yourActivityParam).Get(ctx, &yourActivityResult)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return &yourActivityResult, nil
}

func HeartbeatTimeoutWorkflow(ctx workflow.Context, yourActivityParam YourActivityParam) (*YourActivityResult, error) {
	// @@@SNIPSTART go-activity-heartbeat-timeout
	activityOptions := workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Minute,
		HeartbeatTimeout:    10 * time.Second,
	}
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	var yourActivityResult YourActivityResult
	err := workflow.ExecuteActivity(ctx, YourActivityDefinition, yourActivityParam).Get(ctx, &yourActivityResult)
	if err != nil {
		return nil, err
	}
	// @@@SNIPEND
	return &yourActivityResult, nil
}
