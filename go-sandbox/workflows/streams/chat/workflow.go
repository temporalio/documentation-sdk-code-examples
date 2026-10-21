// @@@SNIPSTART go-streams-chat-workflow
package chat

import (
	"time"

	"go.temporal.io/sdk/contrib/workflowstreams"
	"go.temporal.io/sdk/workflow"
)

type ChatInput struct {
	Prompt      string
	StreamState *workflowstreams.WorkflowStreamState
}

func ChatWorkflow(ctx workflow.Context, input ChatInput) (string, error) {
	// The Workflow hosts the stream. The Activity publishes to it.
	if _, err := workflowstreams.NewWorkflowStream(ctx, input.StreamState); err != nil {
		return "", err
	}

	subscriberDone := false
	ackCh := workflow.GetSignalChannel(ctx, "subscriber-acknowledged-terminator")
	workflow.Go(ctx, func(ctx workflow.Context) {
		ackCh.Receive(ctx, nil)
		subscriberDone = true
	})

	ao := workflow.ActivityOptions{StartToCloseTimeout: 5 * time.Minute}
	ctx = workflow.WithActivityOptions(ctx, ao)
	var result string
	if err := workflow.ExecuteActivity(ctx, StreamCompletion, input.Prompt).Get(ctx, &result); err != nil {
		return "", err
	}

	// Wait for the subscriber to ack the terminal `close` event. The timeout
	// is a fallback for when no subscriber is attached; with the ack, the
	// typical case exits as soon as the subscriber confirms.
	_, _ = workflow.AwaitWithTimeout(ctx, 30*time.Second, func() bool { return subscriberDone })
	return result, nil
}
// @@@SNIPEND
