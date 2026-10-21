// Package replay fetches a Workflow Execution's Event History and replays it.
package replay

import (
	"context"

	"go.temporal.io/api/enums/v1"
	"go.temporal.io/api/history/v1"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
	"go.temporal.io/sdk/workflow"
)

// YourWorkflow is the Workflow Definition under test.
func YourWorkflow(ctx workflow.Context, name string) (string, error) {
	if err := workflow.Sleep(ctx, 0); err != nil {
		return "", err
	}
	return "Hello " + name, nil
}

// @@@SNIPSTART go-testing-get-workflow-history
func GetWorkflowHistory(ctx context.Context, client client.Client, id, runID string) (*history.History, error) {
	var hist history.History
	iter := client.GetWorkflowHistory(ctx, id, runID, false, enums.HISTORY_EVENT_FILTER_TYPE_ALL_EVENT)
	for iter.HasNext() {
		event, err := iter.Next()
		if err != nil {
			return nil, err
		}
		hist.Events = append(hist.Events, event)
	}
	return &hist, nil
}
// @@@SNIPEND

// @@@SNIPSTART go-testing-replay-workflow
func ReplayWorkflow(ctx context.Context, client client.Client, id, runID string) error {
	hist, err := GetWorkflowHistory(ctx, client, id, runID)
	if err != nil {
		return err
	}
	replayer := worker.NewWorkflowReplayer()
	replayer.RegisterWorkflow(YourWorkflow)
	return replayer.ReplayWorkflowHistory(nil, hist)
}
// @@@SNIPEND
