// Package nexusmock shows how to test a Workflow that calls a Nexus Operation.
package nexusmock

import "go.temporal.io/sdk/workflow"

type SampleInput struct {
	Message string
}

type SampleOutput struct {
	Message string
}

// SimpleWorkflow calls the sample-operation Nexus Operation and returns its output.
func SimpleWorkflow(ctx workflow.Context, message string) (SampleOutput, error) {
	c := workflow.NewNexusClient("sample-endpoint", "sample-service")
	var output SampleOutput
	err := c.ExecuteOperation(ctx, "sample-operation", SampleInput{}, workflow.NexusOperationOptions{}).Get(ctx, &output)
	return output, err
}

// SampleHandlerWorkflow backs the overridden asynchronous Nexus Operation in tests.
func SampleHandlerWorkflow(_ workflow.Context, input SampleInput) (SampleOutput, error) {
	// Custom logic here.
	return SampleOutput{}, nil
}
