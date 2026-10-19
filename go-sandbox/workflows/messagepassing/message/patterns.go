package message

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// @@@SNIPSTART go-message-blocking-update
func BlockingGreetingWorkflow(ctx workflow.Context) (string, error) {
	language := English
	greeting := map[Language]string{English: "Hello", Chinese: "你好，世界"}

	err := workflow.SetUpdateHandler(ctx, SetLanguageUpdate, func(ctx workflow.Context, newLanguage Language) (Language, error) {
		if _, ok := greeting[newLanguage]; !ok {
			ao := workflow.ActivityOptions{
				StartToCloseTimeout: 10 * time.Second,
			}
			ctx = workflow.WithActivityOptions(ctx, ao)

			var newGreeting string
			err := workflow.ExecuteActivity(ctx, CallGreetingService, newLanguage).Get(ctx, &newGreeting)
			if err != nil {
				return "", err
			}
			greeting[newLanguage] = newGreeting
		}
		var previousLanguage Language
		previousLanguage, language = language, newLanguage
		return previousLanguage, nil
	})
	if err != nil {
		return "", err
	}
	// ...
	workflow.GetSignalChannel(ctx, ApproveSignal).Receive(ctx, nil)
	if err := workflow.Await(ctx, func() bool { return workflow.AllHandlersFinished(ctx) }); err != nil {
		return "", err
	}
	return greeting[language], nil
}
// @@@SNIPEND

// AwaitInHandlerWorkflow holds Updates until the main Workflow finishes its setup.
func AwaitInHandlerWorkflow(ctx workflow.Context) (int, error) {
	updateUnblocked := false
	updateUnblockedFunc := func() bool { return updateUnblocked }
	handled := 0

	// @@@SNIPSTART go-message-await-in-handler
	err := workflow.SetUpdateHandler(ctx, "UpdateHandler", func(ctx workflow.Context, input UpdateInput) error {
		if err := workflow.Await(ctx, updateUnblockedFunc); err != nil {
			return err
		}
		// ...
		handled++
		return nil
	})
	// @@@SNIPEND
	if err != nil {
		return 0, err
	}

	// Stand-in for setup the Update handler depends on.
	if err := workflow.Sleep(ctx, time.Second); err != nil {
		return 0, err
	}
	updateUnblocked = true

	workflow.GetSignalChannel(ctx, ApproveSignal).Receive(ctx, nil)
	if err := workflow.Await(ctx, func() bool { return workflow.AllHandlersFinished(ctx) }); err != nil {
		return 0, err
	}
	return handled, nil
}

// @@@SNIPSTART go-message-wait-all-handlers
func WaitForHandlersWorkflow(ctx workflow.Context, param YourWorkflowParam) error {
	// ...
	err := workflow.Await(ctx, func() bool {
		return workflow.AllHandlersFinished(ctx)
	})
	if err != nil {
		return err
	}
	return nil
}
// @@@SNIPEND

const UpdateHandlerName = "fire-and-forget"

// UnfinishedPolicyWorkflow completes without waiting for its Update handler.
func UnfinishedPolicyWorkflow(ctx workflow.Context) error {
	UpdateFunc := func(ctx workflow.Context) error {
		// This handler may still be running when the Workflow completes.
		return workflow.Sleep(ctx, time.Hour)
	}

	// @@@SNIPSTART go-message-unfinished-policy
	err := workflow.SetUpdateHandlerWithOptions(ctx, UpdateHandlerName, UpdateFunc, workflow.UpdateHandlerOptions{
		UnfinishedPolicy: workflow.HandlerUnfinishedPolicyAbandon,
	})
	// @@@SNIPEND
	if err != nil {
		return err
	}
	workflow.GetSignalChannel(ctx, ApproveSignal).Receive(ctx, nil)
	return nil
}
