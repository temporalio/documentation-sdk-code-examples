package humanintheloop

import (
	"fmt"
	"time"

	"github.com/google/uuid"
	"go.temporal.io/sdk/workflow"
)

// SystemInstructions tell the LLM how to analyze a request and assess its risk.
const SystemInstructions = `
You are an AI assistant that analyzes user requests and proposes actions.
For each request, you should:
1. Determine what action needs to be taken
2. Provide a clear description of the action
3. Explain your reasoning for why this action addresses the request
4. Assess whether this is a risky action that requires human approval

Consider an action risky if it:
- Could cause data loss or corruption
- Could affect production systems or critical infrastructure
- Could have financial or legal implications
- Could impact user experience or system availability
- Involves deleting, modifying, or overwriting important data
- Could expose sensitive information or security vulnerabilities

Be thorough and clear in your analysis.

Respond with a JSON object in this structure:

{
  "action_type": "A short name for the action (e.g., \"delete_test_data\")",
  "description": "A clear description of what the action will do",
  "reasoning": "Your explanation for why this action addresses the request",
  "risky_action": true or false indicating whether this action is considered risky
}
`

// HumanInTheLoopWorkflow asks an LLM to propose an action for the user's
// request. Safe actions run right away. Risky actions wait for an approval
// decision delivered by the ApprovalSignal Signal, with a durable timeout in
// case no one responds.
func HumanInTheLoopWorkflow(ctx workflow.Context, input WorkflowInput) (string, error) {
	logger := workflow.GetLogger(ctx)
	state := handleApprovalSignals(ctx)

	var proposed ProposedAction
	llmCtx := workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 30 * time.Second,
	})
	err := workflow.ExecuteActivity(llmCtx, ProposeAction, OpenAIResponsesRequest{
		Model:        "gpt-4o-mini",
		Instructions: SystemInstructions,
		Input:        input.UserRequest,
	}).Get(ctx, &proposed)
	if err != nil {
		return "", err
	}
	logger.Info("AI proposed action", "ActionType", proposed.ActionType, "Risky", proposed.RiskyAction)

	if !proposed.RiskyAction {
		result, err := execute(ctx, proposed)
		if err != nil {
			return "", err
		}
		return "Action completed successfully (auto-approved): " + result, nil
	}

	decision, err := requestApproval(ctx, state, proposed, input.UserRequest, input.ApprovalTimeoutSeconds)
	if err != nil {
		return "", err
	}
	if decision == nil {
		return fmt.Sprintf("Action cancelled: approval request timed out after %d seconds",
			input.ApprovalTimeoutSeconds), nil
	}
	if !decision.Approved {
		notes := decision.ReviewerNotes
		if notes == "" {
			notes = "None provided"
		}
		return "Action rejected. Reviewer notes: " + notes, nil
	}

	result, err := execute(ctx, proposed)
	if err != nil {
		return "", err
	}
	return "Action completed successfully: " + result, nil
}

func execute(ctx workflow.Context, action ProposedAction) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 60 * time.Second,
	})
	var result string
	err := workflow.ExecuteActivity(ctx, ExecuteAction, action).Get(ctx, &result)
	return result, err
}

// approvalState is the pending request and the decision for it. The Signal
// goroutine writes it; requestApproval reads it.
type approvalState struct {
	pendingRequestID string
	decision         *ApprovalDecision
}

// handleApprovalSignals receives ApprovalSignal in a Workflow goroutine for the
// life of the Workflow.
func handleApprovalSignals(ctx workflow.Context) *approvalState {
	logger := workflow.GetLogger(ctx)
	state := &approvalState{}
	signals := workflow.GetSignalChannel(ctx, ApprovalSignal)
	workflow.Go(ctx, func(ctx workflow.Context) {
		for {
			var d ApprovalDecision
			signals.Receive(ctx, &d)
			switch {
			case state.pendingRequestID == "":
				logger.Warn("Ignoring decision: no approval request is pending")
			// A decision without a request_id applies to the pending request.
			// One with a request_id must match it, so a stale decision is
			// ignored.
			case d.RequestID != "" && d.RequestID != state.pendingRequestID:
				logger.Warn("Ignoring decision for another request",
					"RequestID", d.RequestID, "Expected", state.pendingRequestID)
			default:
				state.decision = &d
			}
		}
	})
	return state
}

// requestApproval notifies a reviewer and waits for a decision. It returns nil
// if the timeout fires first.
func requestApproval(
	ctx workflow.Context, state *approvalState, action ProposedAction, userRequest string, timeoutSeconds int,
) (*ApprovalDecision, error) {
	// SideEffect records the generated ID in Event History, so replay sees the
	// same value.
	var requestID string
	if err := workflow.SideEffect(ctx, func(workflow.Context) interface{} {
		return uuid.NewString()
	}).Get(&requestID); err != nil {
		return nil, err
	}
	state.pendingRequestID = requestID
	state.decision = nil

	notifyCtx := workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
	})
	err := workflow.ExecuteActivity(notifyCtx, NotifyApprovalNeeded, ApprovalRequest{
		RequestID:      requestID,
		ProposedAction: action,
		Context:        userRequest,
		RequestedAt:    workflow.Now(ctx).UTC().Format(time.RFC3339),
	}).Get(ctx, nil)
	if err != nil {
		return nil, err
	}

	// A durable timer: the Workflow uses no compute while it waits, and the
	// timeout survives Worker restarts.
	ok, err := workflow.AwaitWithTimeout(ctx, time.Duration(timeoutSeconds)*time.Second,
		func() bool { return state.decision != nil })
	if err != nil || !ok {
		return nil, err
	}
	return state.decision, nil
}
