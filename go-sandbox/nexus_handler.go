// @@@SNIPSTART go-nexus-quickstart-handler
package quickstart

import (
	"context"

	"github.com/nexus-rpc/sdk-go/nexus"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/temporalnexus"
	"go.temporal.io/sdk/workflow"
)

// HelloNexusWorkflow is the handler Workflow for the Nexus SayHello Operation.
// It bridges the Nexus HelloInput to SayHelloWorkflow's string parameter.
func HelloNexusWorkflow(ctx workflow.Context, input HelloInput) (string, error) {
	return SayHelloWorkflow(ctx, input.Name)
}

var HelloOperation = temporalnexus.NewWorkflowRunOperation(
	HelloOperationName,
	HelloNexusWorkflow,
	func(ctx context.Context, input HelloInput, options nexus.StartOperationOptions) (client.StartWorkflowOptions, error) {
		return client.StartWorkflowOptions{
			// RequestID is stable across retries, making it safe to use as a Workflow ID.
			ID: options.RequestID,
		}, nil
	},
)
// @@@SNIPEND
