package continueasnew

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// jobsToProcess is the total work the Workflow does across all runs in the chain.
const jobsToProcess = 50

type ClusterManagerState struct {
	JobsProcessed int
}

type ClusterManagerResult struct {
	JobsProcessed int
}

type ClusterManager struct {
	state             ClusterManagerState
	testContinueAsNew bool
	maxHistoryLength  int
}

// @@@SNIPSTART go-continue-as-new-input
type ClusterManagerInput struct {
	State             *ClusterManagerState
	TestContinueAsNew bool
}

func newClusterManager(ctx workflow.Context, wfInput ClusterManagerInput) (*ClusterManager, error) {
	cm := &ClusterManager{testContinueAsNew: wfInput.TestContinueAsNew}
	// The original caller passes a nil State. Later runs receive the state from the previous run.
	if wfInput.State != nil {
		cm.state = *wfInput.State
	}
	// Test hook: use a small history limit so tests reach Continue-As-New quickly.
	if wfInput.TestContinueAsNew {
		cm.maxHistoryLength = 120
	}
	return cm, nil
}
// @@@SNIPEND

func ClusterManagerWorkflow(ctx workflow.Context, wfInput ClusterManagerInput) (ClusterManagerResult, error) {
	cm, err := newClusterManager(ctx, wfInput)
	if err != nil {
		return ClusterManagerResult{}, err
	}

	for cm.state.JobsProcessed < jobsToProcess {
		// Stand-in for a unit of work. Each Timer adds Events to the Event History.
		if err := workflow.Sleep(ctx, time.Second); err != nil {
			return ClusterManagerResult{}, err
		}
		cm.state.JobsProcessed++

		if cm.shouldContinueAsNew(ctx) {
			// Let any Signal or Update handlers finish before continuing as new.
			if err := workflow.Await(ctx, func() bool { return workflow.AllHandlersFinished(ctx) }); err != nil {
				return ClusterManagerResult{}, err
			}
			// @@@SNIPSTART go-continue-as-new-return
			return ClusterManagerResult{}, workflow.NewContinueAsNewError(
				ctx,
				ClusterManagerWorkflow,
				ClusterManagerInput{
					State:             &cm.state,
					TestContinueAsNew: cm.testContinueAsNew,
				},
			)
			// @@@SNIPEND
		}
	}
	return ClusterManagerResult{JobsProcessed: cm.state.JobsProcessed}, nil
}

// @@@SNIPSTART go-continue-as-new-should-continue
func (cm *ClusterManager) shouldContinueAsNew(ctx workflow.Context) bool {
	if workflow.GetInfo(ctx).GetContinueAsNewSuggested() {
		return true
	}
	if cm.maxHistoryLength > 0 && workflow.GetInfo(ctx).GetCurrentHistoryLength() > cm.maxHistoryLength {
		return true
	}
	return false
}
// @@@SNIPEND
