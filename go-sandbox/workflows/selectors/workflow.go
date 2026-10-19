package selectors

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

const channelName = "example-signal"

// @@@SNIPSTART go-selector-api-example
func SampleWorkflow(ctx workflow.Context) error {
	// standard Workflow setup code
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
	})

	// API Example: declare a new selector
	selector := workflow.NewSelector(ctx)

	// API Example: defer code execution until the Future that represents Activity result is ready
	work := workflow.ExecuteActivity(ctx, ExampleActivity)
	selector.AddFuture(work, func(f workflow.Future) {
		// deferred code omitted...
	})

	// more parallel timers and activities initiated...

	// API Example: receive information from a Channel
	var signalVal string
	channel := workflow.GetSignalChannel(ctx, channelName)
	selector.AddReceive(channel, func(c workflow.ReceiveChannel, more bool) {
		// matching on the channel doesn't consume the message.
		// So it has to be explicitly consumed here
		c.Receive(ctx, &signalVal)
		// do something with received information
	})

	// API Example: block until the next Future is ready to run
	// important! none of the deferred code runs until you call selector.Select
	selector.Select(ctx)

	return nil
}
// @@@SNIPEND

// ProcessAllWorkflow runs an Activity for each item in parallel and waits for all of them.
func ProcessAllWorkflow(ctx workflow.Context, items []string) ([]string, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
	})
	selector := workflow.NewSelector(ctx)
	var results []string
	var processErr error
	for _, item := range items {
		selector.AddFuture(workflow.ExecuteActivity(ctx, ProcessItemActivity, item), func(f workflow.Future) {
			var result string
			if err := f.Get(ctx, &result); err != nil {
				processErr = err
				return
			}
			results = append(results, result)
		})
	}

	// @@@SNIPSTART go-selector-select-loop
	// API Example: blocking conditionally
	if len(items) > 0 {
		selector.Select(ctx) // wait for the first Future
	}

	// API Example: popping off all remaining Futures
	for i := 1; i < len(items); i++ {
		selector.Select(ctx) // this will wait for one branch
		// you can interrupt execution here
		if processErr != nil {
			return nil, processErr
		}
	}
	// @@@SNIPEND
	if processErr != nil {
		return nil, processErr
	}
	return results, nil
}

// OrderProcessingWorkflow sends a notification email if order processing takes
// longer than processingTimeThreshold, without canceling the processing.
func OrderProcessingWorkflow(ctx workflow.Context, processingTime, processingTimeThreshold time.Duration) (bool, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Minute,
	})
	selector := workflow.NewSelector(ctx)
	emailSent := false

	// @@@SNIPSTART go-selector-timer-race
	// Create a cancelable Context for the Timer.
	childCtx, cancelHandler := workflow.WithCancel(ctx)

	var processingDone bool
	f := workflow.ExecuteActivity(ctx, OrderProcessingActivity, processingTime)
	selector.AddFuture(f, func(f workflow.Future) {
		processingDone = true
		// cancel timerFuture
		cancelHandler()
	})

	// use timer future to send notification email if processing takes too long
	timerFuture := workflow.NewTimer(childCtx, processingTimeThreshold)
	selector.AddFuture(timerFuture, func(f workflow.Future) {
		if !processingDone {
			// processing is not done yet when timer fires, send notification email
			_ = workflow.ExecuteActivity(ctx, SendEmailActivity).Get(ctx, nil)
			emailSent = true
		}
	})

	// wait the timer or the order processing to finish
	selector.Select(ctx)
	// @@@SNIPEND

	// If the Timer fired first, wait for processing to finish.
	if !processingDone {
		if err := f.Get(ctx, nil); err != nil {
			return emailSent, err
		}
	}
	return emailSent, nil
}
