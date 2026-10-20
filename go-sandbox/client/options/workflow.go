// Package options shows each StartWorkflowOptions field and how to get Workflow results.
package options

import "go.temporal.io/sdk/workflow"

type YourWorkflowParam struct {
	Name string
}

type YourWorkflowResponse struct {
	Greeting string
}

func YourWorkflowDefinition(ctx workflow.Context, param YourWorkflowParam) (YourWorkflowResponse, error) {
	return YourWorkflowResponse{Greeting: "Hello " + param.Name}, nil
}

// @@@SNIPSTART go-client-last-completion-result
type CronResult struct {
	Count int
}

func YourCronWorkflowDefinition(ctx workflow.Context) (CronResult, error) {
	count := 1

	if workflow.HasLastCompletionResult(ctx) {
		var lastResult CronResult
		if err := workflow.GetLastCompletionResult(ctx, &lastResult); err == nil {
			count = count + lastResult.Count
		}
	}

	newResult := CronResult{
		Count: count,
	}
	return newResult, nil
}
// @@@SNIPEND
