package asynccompletion

import (
	"context"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/temporal"
)

// CompleteActivity completes the Activity with a result.
// The result type must match the return type of the Activity Definition.
func CompleteActivity(taskToken []byte, result string) error {
	// @@@SNIPSTART go-async-activity-complete
	// Instantiate a Temporal service client.
	// The same client can be used to complete or fail any number of Activities.
	// The client is a heavyweight object that should be created once per process.
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		return err
	}
	defer temporalClient.Close()

	// Complete the Activity.
	return temporalClient.CompleteActivity(context.Background(), taskToken, result, nil)
	// @@@SNIPEND
}

// FailActivity fails the Activity without starting another attempt.
func FailActivity(temporalClient client.Client, taskToken []byte) error {
	// @@@SNIPSTART go-async-activity-fail
	// Fail the Activity. A non-retryable error stops the Retry Policy from
	// starting another Activity attempt.
	reason := temporal.NewNonRetryableApplicationError("order rejected", "Rejected", nil)
	return temporalClient.CompleteActivity(context.Background(), taskToken, nil, reason)
	// @@@SNIPEND
}

// CompleteActivityByID completes the Activity without a Task Token.
func CompleteActivityByID(temporalClient client.Client, namespace, workflowID, result string) error {
	// @@@SNIPSTART go-async-activity-complete-by-id
	// Identify the Activity by Namespace, Workflow ID, and Activity ID.
	// An empty Run ID targets the latest Run of the Workflow.
	runID := ""
	return temporalClient.CompleteActivityByID(context.Background(), namespace, workflowID, runID, ApprovalActivityID, result, nil)
	// @@@SNIPEND
}
