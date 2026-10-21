// @@@SNIPSTART go-nexus-quickstart-caller-workflow
package quickstart

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

const (
	CallerTaskQueue = "my-caller-task-queue"
	NexusEndpoint   = "my-nexus-endpoint-name"
)

func CallerWorkflow(ctx workflow.Context, name string) (string, error) {
	c := workflow.NewNexusClient(NexusEndpoint, HelloServiceName)

	fut := c.ExecuteOperation(ctx, HelloOperationName, HelloInput{Name: name}, workflow.NexusOperationOptions{
		ScheduleToCloseTimeout: 10 * time.Second,
	})

	var result string
	if err := fut.Get(ctx, &result); err != nil {
		return "", err
	}
	return result, nil
}
// @@@SNIPEND
