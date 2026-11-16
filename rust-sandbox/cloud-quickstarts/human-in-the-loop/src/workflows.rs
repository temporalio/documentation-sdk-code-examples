use std::time::Duration;

use chrono::{DateTime, SecondsFormat, Utc};
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{
    ActivityOptions, CancellableFuture, SyncWorkflowContext, WorkflowContext, WorkflowResult,
    workflows::select,
};

use crate::{
    ApprovalDecision, ApprovalRequest, OpenAIResponsesRequest, ProposedAction, WorkflowInput,
    activities::AgentActivities,
};

/// Tells the LLM how to analyze a request and assess its risk.
pub const SYSTEM_INSTRUCTIONS: &str = r#"
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
"#;

/// An AI agent that asks a human to approve risky actions.
#[workflow]
#[derive(Default)]
pub struct HumanInTheLoopWorkflow {
    // The Signal handler writes these; the wait condition reads them.
    current_decision: Option<ApprovalDecision>,
    pending_request_id: Option<String>,
}

#[workflow_methods]
impl HumanInTheLoopWorkflow {
    /// Returns a sentence describing whether the action ran, was rejected, or
    /// timed out.
    #[run]
    pub async fn run(
        ctx: &mut WorkflowContext<Self>,
        input: WorkflowInput,
    ) -> WorkflowResult<String> {
        let proposed = ctx
            .execute_activity(
                AgentActivities::propose_action,
                OpenAIResponsesRequest {
                    model: "gpt-4o-mini".to_string(),
                    instructions: SYSTEM_INSTRUCTIONS.to_string(),
                    input: input.user_request.clone(),
                },
                ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
            )
            .await?;
        if !ctx.is_replaying() {
            println!(
                "AI proposed action: {} (risky: {})",
                proposed.action_type, proposed.risky_action
            );
        }

        if !proposed.risky_action {
            let result = execute_action(ctx, proposed).await?;
            return Ok(format!(
                "Action completed successfully (auto-approved): {result}"
            ));
        }

        let timeout_seconds = input.approval_timeout_seconds;
        let decision =
            request_approval(ctx, &proposed, &input.user_request, timeout_seconds).await?;
        let Some(decision) = decision else {
            return Ok(format!(
                "Action cancelled: approval request timed out after {timeout_seconds} seconds"
            ));
        };
        if !decision.approved {
            let notes = decision
                .reviewer_notes
                .as_deref()
                .unwrap_or("None provided");
            return Ok(format!("Action rejected. Reviewer notes: {notes}"));
        }
        let result = execute_action(ctx, proposed).await?;
        Ok(format!("Action completed successfully: {result}"))
    }

    /// Delivers a reviewer's approve or reject decision.
    #[signal(name = "approval_decision")]
    pub fn approval_decision(
        &mut self,
        ctx: &mut SyncWorkflowContext<Self>,
        decision: ApprovalDecision,
    ) {
        let warning = match (&self.pending_request_id, &decision.request_id) {
            (None, _) => Some("Ignoring decision: no approval request is pending".to_string()),
            // A decision without a request_id applies to the pending request. One
            // with a request_id must match it, so a stale decision is ignored.
            (Some(_), None) => None,
            (Some(pending), Some(id)) if pending == id => None,
            (Some(pending), Some(id)) => Some(format!(
                "Ignoring decision for request {id}; expected {pending}"
            )),
        };
        match warning {
            None => self.current_decision = Some(decision),
            Some(message) if !ctx.is_replaying() => eprintln!("WARN {message}"),
            Some(_) => {}
        }
    }
}

async fn execute_action(
    ctx: &mut WorkflowContext<HumanInTheLoopWorkflow>,
    action: ProposedAction,
) -> WorkflowResult<String> {
    Ok(ctx
        .execute_activity(
            AgentActivities::execute_action,
            action,
            ActivityOptions::start_to_close_timeout(Duration::from_secs(60)),
        )
        .await?)
}

/// Notifies a reviewer and waits for a decision. Returns `None` if the timeout
/// fires first.
async fn request_approval(
    ctx: &mut WorkflowContext<HumanInTheLoopWorkflow>,
    action: &ProposedAction,
    user_request: &str,
    timeout_seconds: u64,
) -> WorkflowResult<Option<ApprovalDecision>> {
    // uuid4() is deterministic in Workflow code, so it's safe on replay.
    let request_id = ctx.uuid4();
    ctx.state_mut(|s| {
        s.current_decision = None;
        s.pending_request_id = Some(request_id.clone());
    });

    // Workflow time is deterministic, unlike the system clock.
    let requested_at = ctx
        .workflow_time()
        .map(|time| DateTime::<Utc>::from(time).to_rfc3339_opts(SecondsFormat::Secs, true))
        .unwrap_or_default();
    ctx.execute_activity(
        AgentActivities::notify_approval_needed,
        ApprovalRequest {
            request_id,
            proposed_action: action.clone(),
            context: user_request.to_string(),
            requested_at,
        },
        ActivityOptions::start_to_close_timeout(Duration::from_secs(10)),
    )
    .await?;

    // A durable timer: the Workflow uses no compute while it waits, and the
    // timeout survives Worker restarts. Whichever finishes first wins.
    let mut timer = std::pin::pin!(ctx.timer(Duration::from_secs(timeout_seconds)));
    let decided = select! {
        waited = ctx.wait_condition(|s| s.current_decision.is_some()) => { waited?; true }
        _ = timer => false,
    };
    if decided {
        // The decision arrived first, so the timer is no longer needed.
        timer.cancel();
    }
    Ok(ctx
        .state(|s| s.current_decision.clone())
        .filter(|_| decided))
}
