package versioning

import "go.temporal.io/sdk/workflow"

// Each function below is YourWorkflow at a later stage of patching the "Step1" change.
// In a real application, each stage replaces the previous one in the same function.

// YourWorkflowV1 replaces ActivityA with ActivityC.
func YourWorkflowV1(ctx workflow.Context, data string) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	var result1 string

	// @@@SNIPSTART go-versioning-get-version
	var err error
	v := workflow.GetVersion(ctx, "Step1", workflow.DefaultVersion, 1)
	if v == workflow.DefaultVersion {
		err = workflow.ExecuteActivity(ctx, ActivityA, data).Get(ctx, &result1)
	} else {
		err = workflow.ExecuteActivity(ctx, ActivityC, data).Get(ctx, &result1)
	}
	if err != nil {
		return "", err
	}

	var result2 string
	err = workflow.ExecuteActivity(ctx, ActivityB, result1).Get(ctx, &result2)
	return result2, err
	// @@@SNIPEND
}

// YourWorkflowV2 replaces ActivityC with ActivityD.
func YourWorkflowV2(ctx workflow.Context, data string) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	var result1 string
	var err error

	// @@@SNIPSTART go-versioning-second-change
	v := workflow.GetVersion(ctx, "Step1", workflow.DefaultVersion, 2)
	if v == workflow.DefaultVersion {
		err = workflow.ExecuteActivity(ctx, ActivityA, data).Get(ctx, &result1)
	} else if v == 1 {
		err = workflow.ExecuteActivity(ctx, ActivityC, data).Get(ctx, &result1)
	} else {
		err = workflow.ExecuteActivity(ctx, ActivityD, data).Get(ctx, &result1)
	}
	// @@@SNIPEND
	return runActivityB(ctx, result1, err)
}

// YourWorkflowV3 removes the code for DefaultVersion.
func YourWorkflowV3(ctx workflow.Context, data string) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	var result1 string
	var err error

	// @@@SNIPSTART go-versioning-remove-default
	v := workflow.GetVersion(ctx, "Step1", 1, 2)
	if v == 1 {
		err = workflow.ExecuteActivity(ctx, ActivityC, data).Get(ctx, &result1)
	} else {
		err = workflow.ExecuteActivity(ctx, ActivityD, data).Get(ctx, &result1)
	}
	// @@@SNIPEND
	return runActivityB(ctx, result1, err)
}

// YourWorkflowV4 removes the code for version 1 and keeps the GetVersion call.
func YourWorkflowV4(ctx workflow.Context, data string) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	var result1 string
	var err error

	// @@@SNIPSTART go-versioning-keep-get-version
	workflow.GetVersion(ctx, "Step1", 2, 2)
	err = workflow.ExecuteActivity(ctx, ActivityD, data).Get(ctx, &result1)
	// @@@SNIPEND
	return runActivityB(ctx, result1, err)
}

// YourWorkflowV5 removes the "Step1" GetVersion call and patches a later change with a new change ID.
func YourWorkflowV5(ctx workflow.Context, data string) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	var result1 string
	var err error

	// @@@SNIPSTART go-versioning-new-change-id
	v := workflow.GetVersion(ctx, "Step1-fix2", workflow.DefaultVersion, 1)
	if v == workflow.DefaultVersion {
		err = workflow.ExecuteActivity(ctx, ActivityD, data).Get(ctx, &result1)
	} else {
		err = workflow.ExecuteActivity(ctx, ActivityE, data).Get(ctx, &result1)
	}
	// @@@SNIPEND
	return runActivityB(ctx, result1, err)
}

func runActivityB(ctx workflow.Context, result1 string, err error) (string, error) {
	if err != nil {
		return "", err
	}
	var result2 string
	err = workflow.ExecuteActivity(ctx, ActivityB, result1).Get(ctx, &result2)
	return result2, err
}
