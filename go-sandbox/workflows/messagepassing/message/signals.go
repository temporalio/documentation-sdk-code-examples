package message

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// @@@SNIPSTART go-message-signal-async
func SignalAsyncWorkflow(ctx workflow.Context, param YourWorkflowParam) error {
	var signal MySignal
	signalChan := workflow.GetSignalChannel(ctx, "your-signal-name")
	workflow.Go(ctx, func(ctx workflow.Context) {
		for {
			selector := workflow.NewSelector(ctx)
			selector.AddReceive(signalChan, func(c workflow.ReceiveChannel, more bool) {
				c.Receive(ctx, &signal)
				workflow.GetLogger(ctx).Info("Received Signal", "Message", signal.Message)
			})
			selector.Select(ctx)
		}
	})
	// You could now submit an activity; any signals will still be received while the activity is pending.
	return workflow.Sleep(ctx, time.Minute)
}
// @@@SNIPEND

type completionTracker struct {
	completed map[int]bool
}

func (s *completionTracker) recordCompletion(ctx workflow.Context, recordID int) {
	s.completed[recordID] = true
}

// DrainSignalsWorkflow processes every pending ReportCompletion Signal before it completes.
func DrainSignalsWorkflow(ctx workflow.Context) (int, error) {
	s := &completionTracker{completed: map[int]bool{}}
	// Stand-in for the Workflow's main work, during which Signals arrive.
	if err := workflow.Sleep(ctx, time.Minute); err != nil {
		return 0, err
	}

	// @@@SNIPSTART go-message-signal-drain
	reportCompletionChannel := workflow.GetSignalChannel(ctx, "ReportCompletion")
	// Drain signals async
	for {
		var recordID int
		ok := reportCompletionChannel.ReceiveAsync(&recordID)
		if !ok {
			break
		}
		s.recordCompletion(ctx, recordID)
	}
	// @@@SNIPEND
	return len(s.completed), nil
}

// @@@SNIPSTART go-message-signal-external
func SendExternalSignalWorkflow(ctx workflow.Context, param YourWorkflowParam) error {
	// ...
	signal := MySignal{
		Message: "Some important data",
	}
	err := workflow.SignalExternalWorkflow(ctx, "some-workflow-id", "", "your-signal-name", signal).Get(ctx, nil)
	if err != nil {
		return err
	}
	// ...
	return nil
}
// @@@SNIPEND
