/** Keep the Worker and clients pointed at the same value. */
export const TASK_QUEUE =
  process.env.TEMPORAL_TASK_QUEUE || "quickstart-human-in-the-loop";

/**
 * A fixed Workflow ID, so approval commands can be written as fixed text. Only one
 * Workflow with this ID can run at a time.
 */
export const WORKFLOW_ID =
  process.env.HUMAN_IN_THE_LOOP_WORKFLOW_ID || "human-in-the-loop";

// The property names are snake_case so Signal input from the Temporal CLI or UI, for
// example {"approved": true}, matches the other language versions of this sample.

/** Input for humanInTheLoopWorkflow. */
export interface WorkflowInput {
  user_request: string;
  approval_timeout_seconds: number;
}

/** Action the LLM proposes in response to the user request. */
export interface ProposedAction {
  action_type: string;
  description: string;
  reasoning: string;
  risky_action: boolean;
}

/** Request sent to a human reviewer. */
export interface ApprovalRequest {
  request_id: string;
  proposed_action: ProposedAction;
  context: string;
  requested_at: string;
}

/**
 * Decision a human reviewer sends back with a Signal. Leave out request_id to apply
 * the decision to whichever request is pending.
 */
export interface ApprovalDecision {
  approved: boolean;
  request_id?: string;
  reviewer_notes?: string;
  decided_at?: string;
}
