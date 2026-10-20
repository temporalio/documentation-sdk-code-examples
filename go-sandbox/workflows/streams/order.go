// @@@SNIPSTART go-streams-order-workflow
package streams

import (
	"time"

	"go.temporal.io/sdk/contrib/workflowstreams"
	"go.temporal.io/sdk/workflow"
)

type OrderInput struct {
	OrderID     string
	StreamState *workflowstreams.WorkflowStreamState
}

type StatusEvent struct {
	State    string
	Progress int
	Detail   string
}

func OrderWorkflow(ctx workflow.Context, input OrderInput) error {
	stream, err := workflowstreams.NewWorkflowStream(ctx, input.StreamState)
	if err != nil {
		return err
	}
	status := stream.Topic("status")
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{StartToCloseTimeout: time.Minute})

	if err := status.Publish(StatusEvent{State: "validating", Detail: "checking inventory"}); err != nil {
		return err
	}
	if err := workflow.ExecuteActivity(ctx, ValidateOrder, input.OrderID).Get(ctx, nil); err != nil {
		return err
	}

	if err := status.Publish(StatusEvent{State: "charging", Progress: 33, Detail: "authorizing payment"}); err != nil {
		return err
	}
	if err := workflow.ExecuteActivity(ctx, ChargePayment, input.OrderID).Get(ctx, nil); err != nil {
		return err
	}

	if err := status.Publish(StatusEvent{State: "shipping", Progress: 66, Detail: "dispatching to warehouse"}); err != nil {
		return err
	}
	if err := workflow.ExecuteActivity(ctx, DispatchOrder, input.OrderID).Get(ctx, nil); err != nil {
		return err
	}

	if err := status.Publish(StatusEvent{State: "completed", Progress: 100}); err != nil {
		return err
	}
	// Give in-flight subscriber polls time to fetch the terminator before the Workflow returns.
	if err := workflow.Sleep(ctx, 30*time.Second); err != nil {
		return err
	}
	return nil
}
// @@@SNIPEND
