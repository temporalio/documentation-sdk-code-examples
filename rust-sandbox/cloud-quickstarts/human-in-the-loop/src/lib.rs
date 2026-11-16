//! An AI agent that asks a human to approve risky actions.
//!
//! An LLM proposes an action for the user's request. Safe actions run right away.
//! Risky actions wait for an approval decision delivered by a Signal, with a
//! durable timeout in case no one responds.

pub mod activities;
pub mod workflows;

use serde::{Deserialize, Serialize};

/// The Task Queue the Worker polls and the clients target. Keep the Worker and
/// clients pointed at the same value.
pub fn task_queue() -> String {
    env_or("TEMPORAL_TASK_QUEUE", "quickstart-human-in-the-loop")
}

/// A fixed Workflow ID, so approval commands can be written as fixed text. Only
/// one Workflow with this ID can run at a time.
pub fn workflow_id() -> String {
    env_or("HUMAN_IN_THE_LOOP_WORKFLOW_ID", "human-in-the-loop")
}

fn env_or(key: &str, fallback: &str) -> String {
    std::env::var(key)
        .ok()
        .filter(|value| !value.is_empty())
        .unwrap_or_else(|| fallback.to_string())
}

// The field names serialize as-is (snake_case), so Signal input from the Temporal
// CLI or UI, for example {"approved": true}, matches the other language versions
// of this sample.

/// Input for [`workflows::HumanInTheLoopWorkflow`].
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct WorkflowInput {
    pub user_request: String,
    pub approval_timeout_seconds: u64,
}

/// Input for the `propose_action` Activity.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct OpenAIResponsesRequest {
    pub model: String,
    pub instructions: String,
    pub input: String,
}

/// Action the LLM proposes in response to the user request.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct ProposedAction {
    pub action_type: String,
    pub description: String,
    pub reasoning: String,
    pub risky_action: bool,
}

/// Request sent to a human reviewer.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct ApprovalRequest {
    pub request_id: String,
    pub proposed_action: ProposedAction,
    pub context: String,
    pub requested_at: String,
}

/// Decision a human reviewer sends back with a Signal. Leave out `request_id` to
/// apply the decision to whichever request is pending.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct ApprovalDecision {
    pub approved: bool,
    #[serde(default)]
    pub request_id: Option<String>,
    #[serde(default)]
    pub reviewer_notes: Option<String>,
    #[serde(default)]
    pub decided_at: Option<String>,
}
