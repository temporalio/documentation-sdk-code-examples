package handler

import (
	"context"
	"fmt"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/temporalnexus"
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/nexus/service"
)

// @@@SNIPSTART go-nexus-sync-operation
var EchoOperation = temporalnexus.MustNewTemporalOperation(
	temporalnexus.TemporalOperationOptions[service.EchoInput, service.EchoOutput]{
		Name: service.EchoOperationName,
		Start: func(
			ctx context.Context,
			nc temporalnexus.NexusClient,
			input service.EchoInput,
			options temporalnexus.StartTemporalOperationOptions,
		) (temporalnexus.TemporalOperationResult[service.EchoOutput], error) {
			return temporalnexus.NewSyncResult(service.EchoOutput(input)), nil
		},
	})
// @@@SNIPEND

// @@@SNIPSTART go-nexus-signal-operation
var ApproveOperation = temporalnexus.MustNewTemporalOperation(
	temporalnexus.TemporalOperationOptions[service.ApproveInput, service.ApproveOutput]{
		Name: service.ApproveOperationName,
		Start: func(
			ctx context.Context,
			nc temporalnexus.NexusClient,
			input service.ApproveInput,
			options temporalnexus.StartTemporalOperationOptions,
		) (temporalnexus.TemporalOperationResult[service.ApproveOutput], error) {
			err := nc.GetWorkflowClient().SignalWorkflow(
				ctx, GetWorkflowID(input.UserID), "", service.ApproveSignalName, input)
			if err != nil {
				return temporalnexus.TemporalOperationResult[service.ApproveOutput]{}, err
			}
			return temporalnexus.NewSyncResult(service.ApproveOutput{}), nil
		},
	})
// @@@SNIPEND

// @@@SNIPSTART go-nexus-async-operation
var HelloOperation = temporalnexus.MustNewTemporalOperation(
	temporalnexus.TemporalOperationOptions[service.HelloInput, service.HelloOutput]{
		Name: service.HelloOperationName,
		Start: func(
			ctx context.Context,
			nc temporalnexus.NexusClient,
			input service.HelloInput,
			options temporalnexus.StartTemporalOperationOptions,
		) (temporalnexus.TemporalOperationResult[service.HelloOutput], error) {
			return temporalnexus.StartWorkflow(ctx, nc, client.StartWorkflowOptions{
				ID: service.HelloWorkflowID(input),
				// Task queue defaults to the task queue this operation is handled on.
			}, HelloHandlerWorkflow, input)
		},
	})
// @@@SNIPEND

// HelloWithArgsOperation starts a Workflow that takes the name and language as separate arguments.
var HelloWithArgsOperation = temporalnexus.MustNewTemporalOperation(
	temporalnexus.TemporalOperationOptions[service.HelloInput, service.HelloOutput]{
		Name: service.HelloWithArgsOperationName,
		Start: func(
			ctx context.Context,
			nc temporalnexus.NexusClient,
			input service.HelloInput,
			options temporalnexus.StartTemporalOperationOptions,
		) (temporalnexus.TemporalOperationResult[service.HelloOutput], error) {
			// @@@SNIPSTART go-nexus-untyped-workflow
			return temporalnexus.StartUntypedWorkflow[service.HelloOutput](ctx, nc, client.StartWorkflowOptions{
				ID: service.HelloWorkflowID(input),
			}, HelloHandlerWorkflowWithArgs, input.Name, input.Language)
			// @@@SNIPEND
		},
	})

func HelloHandlerWorkflow(_ workflow.Context, input service.HelloInput) (service.HelloOutput, error) {
	return greet(input.Name, input.Language)
}

func HelloHandlerWorkflowWithArgs(_ workflow.Context, name string, language service.Language) (service.HelloOutput, error) {
	return greet(name, language)
}

func greet(name string, language service.Language) (service.HelloOutput, error) {
	switch language {
	case service.EN:
		return service.HelloOutput{Message: "Hello " + name + " 👋"}, nil
	case service.FR:
		return service.HelloOutput{Message: "Bonjour " + name + " 👋"}, nil
	case service.DE:
		return service.HelloOutput{Message: "Hallo " + name + " 👋"}, nil
	case service.ES:
		return service.HelloOutput{Message: "¡Hola! " + name + " 👋"}, nil
	case service.TR:
		return service.HelloOutput{Message: "Merhaba " + name + " 👋"}, nil
	}
	return service.HelloOutput{}, fmt.Errorf("unsupported language %q", language)
}

// GetWorkflowID returns the ID of the approval Workflow for a user.
func GetWorkflowID(userID string) string {
	return "approval-" + userID
}

// ApprovalWorkflow waits for the Approve operation to Signal it.
func ApprovalWorkflow(ctx workflow.Context, userID string) (string, error) {
	var input service.ApproveInput
	workflow.GetSignalChannel(ctx, service.ApproveSignalName).Receive(ctx, &input)
	return "approved " + input.UserID, nil
}
