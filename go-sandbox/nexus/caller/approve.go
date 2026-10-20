package caller

import (
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/nexus/service"
)

// HelloWithArgsCallerWorkflow calls the Operation backed by a Workflow that takes several arguments.
func HelloWithArgsCallerWorkflow(ctx workflow.Context, name string, language service.Language) (string, error) {
	c := workflow.NewNexusClient(endpointName, service.HelloServiceName)
	var res service.HelloOutput
	err := c.ExecuteOperation(ctx, service.HelloWithArgsOperationName, service.HelloInput{Name: name, Language: language}, workflow.NexusOperationOptions{}).Get(ctx, &res)
	return res.Message, err
}

// ApproveCallerWorkflow approves a user through the Nexus Approve operation.
func ApproveCallerWorkflow(ctx workflow.Context, userID string) error {
	c := workflow.NewNexusClient(endpointName, service.HelloServiceName)
	return c.ExecuteOperation(ctx, service.ApproveOperationName, service.ApproveInput{UserID: userID}, workflow.NexusOperationOptions{}).Get(ctx, nil)
}
