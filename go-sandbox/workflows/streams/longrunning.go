package streams

import (
	"time"

	"go.temporal.io/sdk/contrib/workflowstreams"
	"go.temporal.io/sdk/workflow"
)

// doOneIteration stands in for one unit of the Workflow's work.
func doOneIteration(ctx workflow.Context, stream *workflowstreams.WorkflowStream) error {
	if err := workflow.Sleep(ctx, time.Second); err != nil {
		return err
	}
	return stream.Topic("progress").Publish(ProgressEvent{Message: "processed an item"})
}

// @@@SNIPSTART go-streams-continue-as-new
type WorkflowInput struct {
	ItemsProcessed int
	StreamState    *workflowstreams.WorkflowStreamState
}

func LongRunningWorkflow(ctx workflow.Context, input WorkflowInput) error {
	stream, err := workflowstreams.NewWorkflowStream(ctx, input.StreamState)
	if err != nil {
		return err
	}
	itemsProcessed := input.ItemsProcessed

	for {
		if err := doOneIteration(ctx, stream); err != nil {
			return err
		}
		itemsProcessed++

		if workflow.GetInfo(ctx).GetContinueAsNewSuggested() {
			return stream.NewContinueAsNewError(ctx, LongRunningWorkflow, func(state *workflowstreams.WorkflowStreamState) []any {
				return []any{WorkflowInput{
					ItemsProcessed: itemsProcessed,
					StreamState:    state,
				}}
			})
		}
	}
}
// @@@SNIPEND

// LongRunningWorkflowOtherQueue continues as new on another Task Queue with a custom publisher TTL.
func LongRunningWorkflowOtherQueue(ctx workflow.Context, input WorkflowInput) error {
	stream, err := workflowstreams.NewWorkflowStream(ctx, input.StreamState)
	if err != nil {
		return err
	}
	itemsProcessed := input.ItemsProcessed

	for {
		if err := doOneIteration(ctx, stream); err != nil {
			return err
		}
		itemsProcessed++

		if workflow.GetInfo(ctx).GetContinueAsNewSuggested() {
			// @@@SNIPSTART go-streams-continue-as-new-explicit
			stream.DetachPollers()
			if err := workflow.Await(ctx, func() bool { return workflow.AllHandlersFinished(ctx) }); err != nil {
				return err
			}
			state, err := stream.GetState(30 * time.Minute) // custom publisher TTL
			if err != nil {
				return err
			}
			ctx = workflow.WithWorkflowTaskQueue(ctx, "other-tq")
			return workflow.NewContinueAsNewError(ctx, LongRunningWorkflow, WorkflowInput{
				ItemsProcessed: itemsProcessed,
				StreamState:    state,
			})
			// @@@SNIPEND
		}
	}
}
