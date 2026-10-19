package timers

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

const CancelTimerSignal = "cancel-timer"

// TimerWorkflow waits for a Timer, unless it receives a Signal to cancel the Timer first.
// It returns true if the Timer fired.
func TimerWorkflow(ctx workflow.Context, duration time.Duration) (bool, error) {
	// @@@SNIPSTART go-timer-new-timer
	// Use a cancelable Context so the Workflow can cancel the Timer early.
	timerCtx, cancelTimer := workflow.WithCancel(ctx)
	defer cancelTimer()
	timer := workflow.NewTimer(timerCtx, duration)
	// @@@SNIPEND

	timerFired := false
	selector := workflow.NewSelector(ctx)
	selector.AddFuture(timer, func(f workflow.Future) {
		// Get returns a CanceledError if the Timer was canceled.
		timerFired = f.Get(timerCtx, nil) == nil
	})
	selector.AddReceive(workflow.GetSignalChannel(ctx, CancelTimerSignal), func(c workflow.ReceiveChannel, more bool) {
		c.Receive(ctx, nil)
		cancelTimer()
	})
	selector.Select(ctx)
	return timerFired, nil
}

func SleepWorkflow(ctx workflow.Context) error {
	// @@@SNIPSTART go-timer-sleep
	err := workflow.Sleep(ctx, 10*time.Second)
	if err != nil {
		return err
	}
	// @@@SNIPEND
	return nil
}
