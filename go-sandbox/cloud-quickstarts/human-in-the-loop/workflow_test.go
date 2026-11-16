package humanintheloop_test

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.temporal.io/sdk/testsuite"

	humanintheloop "my-org/quickstart/cloud-quickstarts/human-in-the-loop"
)

var (
	safe = humanintheloop.ProposedAction{
		ActionType:  "list_files",
		Description: "List directory contents",
		Reasoning:   "Read-only operation",
		RiskyAction: false,
	}
	risky = humanintheloop.ProposedAction{
		ActionType:  "delete_data",
		Description: "Delete production data",
		Reasoning:   "User asked for deletion",
		RiskyAction: true,
	}
)

// decision is the reviewer's response to send once approval is requested.
type decision struct {
	approved      bool
	notes         string
	withRequestID bool
}

// runWorkflow runs the Workflow with mocked Activities, so the tests need no
// API key. The test environment skips time, so the approval timeout fires
// instantly.
func runWorkflow(t *testing.T, action humanintheloop.ProposedAction, timeoutSeconds int, d *decision) string {
	var ts testsuite.WorkflowTestSuite
	env := ts.NewTestWorkflowEnvironment()
	env.RegisterActivity(humanintheloop.ProposeAction)
	env.RegisterActivity(humanintheloop.NotifyApprovalNeeded)
	env.RegisterActivity(humanintheloop.ExecuteAction)

	env.OnActivity(humanintheloop.ProposeAction, mock.Anything, mock.Anything).Return(action, nil)
	env.OnActivity(humanintheloop.ExecuteAction, mock.Anything, mock.Anything).Return(
		func(_ context.Context, a humanintheloop.ProposedAction) (string, error) {
			return "Executed: " + a.ActionType, nil
		})

	var requestID string
	env.OnActivity(humanintheloop.NotifyApprovalNeeded, mock.Anything, mock.Anything).Return(
		func(_ context.Context, r humanintheloop.ApprovalRequest) error {
			requestID = r.RequestID
			return nil
		})

	if d != nil {
		// Fires after NotifyApprovalNeeded, while the Workflow waits.
		env.RegisterDelayedCallback(func() {
			sig := humanintheloop.ApprovalDecision{Approved: d.approved, ReviewerNotes: d.notes}
			if d.withRequestID {
				sig.RequestID = requestID
			}
			env.SignalWorkflow(humanintheloop.ApprovalSignal, sig)
		}, time.Second)
	}

	env.ExecuteWorkflow(humanintheloop.HumanInTheLoopWorkflow, humanintheloop.WorkflowInput{
		UserRequest:            "test",
		ApprovalTimeoutSeconds: timeoutSeconds,
	})

	require.True(t, env.IsWorkflowCompleted())
	require.NoError(t, env.GetWorkflowError())
	var result string
	require.NoError(t, env.GetWorkflowResult(&result))
	return result
}

func TestSafeActionIsAutoApproved(t *testing.T) {
	result := runWorkflow(t, safe, 30, nil)
	require.Equal(t, "Action completed successfully (auto-approved): Executed: list_files", result)
}

func TestRiskyActionRunsWhenApproved(t *testing.T) {
	result := runWorkflow(t, risky, 300, &decision{approved: true, notes: "Looks good", withRequestID: true})
	require.Equal(t, "Action completed successfully: Executed: delete_data", result)
}

func TestDecisionWithoutRequestIDAppliesToPendingRequest(t *testing.T) {
	result := runWorkflow(t, risky, 300, &decision{approved: true, notes: "Looks good"})
	require.Equal(t, "Action completed successfully: Executed: delete_data", result)
}

func TestRiskyActionIsRejected(t *testing.T) {
	result := runWorkflow(t, risky, 300, &decision{approved: false, notes: "Too risky", withRequestID: true})
	require.Equal(t, "Action rejected. Reviewer notes: Too risky", result)
}

func TestRiskyActionTimesOutWithoutADecision(t *testing.T) {
	result := runWorkflow(t, risky, 1, nil)
	require.Contains(t, result, "timed out after 1 seconds")
}

func TestProposeActionReturnsRiskyMockWithoutAPIKey(t *testing.T) {
	t.Setenv("OPENAI_API_KEY", "")
	var ts testsuite.WorkflowTestSuite
	env := ts.NewTestActivityEnvironment()
	env.RegisterActivity(humanintheloop.ProposeAction)

	val, err := env.ExecuteActivity(humanintheloop.ProposeAction, humanintheloop.OpenAIResponsesRequest{
		Model: "gpt-4o-mini",
		Input: "test",
	})
	require.NoError(t, err)
	var action humanintheloop.ProposedAction
	require.NoError(t, val.Get(&action))
	require.True(t, action.RiskyAction)
	require.Equal(t, "delete_test_data", action.ActionType)
}
