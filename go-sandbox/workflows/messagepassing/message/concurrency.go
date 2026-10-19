package message

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// @@@SNIPSTART go-message-handler-concurrency-bad
func BadHandlerWorkflow(ctx workflow.Context, param YourWorkflowParam) (Data, error) {
	var x, y int
	// ...
	err := workflow.SetUpdateHandler(ctx, "BadUpdateHandler", func(ctx workflow.Context) error {
		ao := workflow.ActivityOptions{
			StartToCloseTimeout: 10 * time.Second,
		}
		ctx = workflow.WithActivityOptions(ctx, ao)

		var result Data
		err := workflow.ExecuteActivity(ctx, FetchData, param.Name).Get(ctx, &result)
		if err != nil {
			return err
		}
		x = result.X
		// 🐛🐛 Bug!! If multiple instances of this handler are executing concurrently, then
		// there may be times when the Workflow has x from one Activity execution and y from another.
		err = workflow.Sleep(ctx, time.Second)
		if err != nil {
			return err
		}
		y = result.Y
		return nil
	})
	if err != nil {
		return Data{}, err
	}
	// ...
	workflow.GetSignalChannel(ctx, ApproveSignal).Receive(ctx, nil)
	if err := workflow.Await(ctx, func() bool { return workflow.AllHandlersFinished(ctx) }); err != nil {
		return Data{}, err
	}
	return Data{X: x, Y: y}, nil
}
// @@@SNIPEND

// @@@SNIPSTART go-message-handler-concurrency-mutex
func SafeHandlerWorkflow(ctx workflow.Context, param YourWorkflowParam) (Data, error) {
	var x, y int
	mutex := workflow.NewMutex(ctx)
	// ...
	err := workflow.SetUpdateHandler(ctx, "SafeUpdateHandler", func(ctx workflow.Context) error {
		err := mutex.Lock(ctx)
		if err != nil {
			return err
		}
		defer mutex.Unlock()
		ao := workflow.ActivityOptions{
			StartToCloseTimeout: 10 * time.Second,
		}
		ctx = workflow.WithActivityOptions(ctx, ao)

		var result Data
		err = workflow.ExecuteActivity(ctx, FetchData, param.Name).Get(ctx, &result)
		if err != nil {
			return err
		}
		x = result.X
		// ✅ OK: the scheduler may switch now to a different handler execution, or to the main workflow
		// method, but no other execution of this handler can run until this execution finishes.
		err = workflow.Sleep(ctx, time.Second)
		if err != nil {
			return err
		}
		y = result.Y
		return nil
	})
	if err != nil {
		return Data{}, err
	}
	// ...
	workflow.GetSignalChannel(ctx, ApproveSignal).Receive(ctx, nil)
	if err := workflow.Await(ctx, func() bool { return workflow.AllHandlersFinished(ctx) }); err != nil {
		return Data{}, err
	}
	return Data{X: x, Y: y}, nil
}
// @@@SNIPEND
