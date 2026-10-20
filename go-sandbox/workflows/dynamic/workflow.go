package dynamic

import (
	"fmt"
	"time"

	"go.temporal.io/sdk/converter"
	"go.temporal.io/sdk/workflow"
)

const TaskQueue = "dynamic-workflow"

// @@@SNIPSTART go-dynamic-workflow-definition
func DynamicWorkflow(ctx workflow.Context, args converter.EncodedValues) (string, error) {
	var result string
	info := workflow.GetInfo(ctx)

	var arg1, arg2 string
	err := args.Get(&arg1, &arg2)
	if err != nil {
		return "", fmt.Errorf("failed to decode arguments: %w", err)
	}

	if info.WorkflowType.Name == "dynamic-activity" {
		ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{StartToCloseTimeout: 10 * time.Second})
		err := workflow.ExecuteActivity(ctx, "random-activity-name", arg1, arg2).Get(ctx, &result)
		if err != nil {
			return "", err
		}
	} else {
		result = fmt.Sprintf("%s - %s - %s", info.WorkflowType.Name, arg1, arg2)
	}

	return result, nil
}
// @@@SNIPEND
