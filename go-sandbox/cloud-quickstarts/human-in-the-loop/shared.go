// Package humanintheloop is a minimal sample of an AI agent that asks a human
// to approve risky actions. An LLM proposes an action, safe actions run right
// away, and risky actions wait for an approval decision delivered by a Signal,
// with a durable timeout in case no one responds.
package humanintheloop

import "os"

// TaskQueue is the Task Queue the Worker polls and the client targets. Keep the
// Worker and client pointed at the same value.
var TaskQueue = envOr("TEMPORAL_TASK_QUEUE", "quickstart-human-in-the-loop")

// WorkflowID is fixed, so approval commands can be written as fixed text. Only
// one Workflow with this ID can run at a time.
var WorkflowID = envOr("HUMAN_IN_THE_LOOP_WORKFLOW_ID", "human-in-the-loop")

// ApprovalSignal is the name of the Signal that carries an ApprovalDecision.
const ApprovalSignal = "approval_decision"

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

// WorkflowInput is the input for HumanInTheLoopWorkflow.
type WorkflowInput struct {
	UserRequest            string `json:"user_request"`
	ApprovalTimeoutSeconds int    `json:"approval_timeout_seconds"`
}

// ProposedAction is the action the LLM proposes in response to the user request.
type ProposedAction struct {
	ActionType  string `json:"action_type"`
	Description string `json:"description"`
	Reasoning   string `json:"reasoning"`
	RiskyAction bool   `json:"risky_action"`
}

// ApprovalRequest is the request sent to a human reviewer.
type ApprovalRequest struct {
	RequestID      string         `json:"request_id"`
	ProposedAction ProposedAction `json:"proposed_action"`
	Context        string         `json:"context"`
	RequestedAt    string         `json:"requested_at"`
}

// ApprovalDecision is the decision a human reviewer sends back with a Signal.
// The JSON field names are what `temporal workflow signal --input` and the
// Temporal UI expect, for example {"approved": true}.
//
// Leave RequestID empty to apply the decision to whichever request is pending.
type ApprovalDecision struct {
	Approved      bool   `json:"approved"`
	RequestID     string `json:"request_id,omitempty"`
	ReviewerNotes string `json:"reviewer_notes,omitempty"`
	DecidedAt     string `json:"decided_at,omitempty"`
}
