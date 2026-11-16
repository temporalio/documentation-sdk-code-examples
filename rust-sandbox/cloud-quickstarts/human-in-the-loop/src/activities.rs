use std::time::Duration;

use serde_json::json;
use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};

use crate::{ApprovalRequest, OpenAIResponsesRequest, ProposedAction};

const RESPONSES_URL: &str = "https://api.openai.com/v1/responses";

/// Activities for the human-in-the-loop agent.
pub struct AgentActivities;

/// A fixed, risky action used when no OpenAI API key is set, so the sample runs
/// end to end (including the approval step) without an LLM.
pub fn mock_proposed_action(user_request: &str) -> ProposedAction {
    ProposedAction {
        action_type: "delete_test_data".to_string(),
        description: format!("Mock action for the request: {user_request}"),
        reasoning: "OPENAI_API_KEY is not set, so this action is mocked instead of LLM-generated."
            .to_string(),
        risky_action: true,
    }
}

#[activities]
impl AgentActivities {
    /// Asks the LLM for a proposed action. If OPENAI_API_KEY isn't set, returns
    /// [`mock_proposed_action`] instead.
    #[activity(name = "propose_action")]
    pub async fn propose_action(
        _ctx: ActivityContext,
        request: OpenAIResponsesRequest,
    ) -> Result<ProposedAction, ActivityError> {
        let api_key = std::env::var("OPENAI_API_KEY").unwrap_or_default();
        if api_key.is_empty() {
            eprintln!(
                "WARN OPENAI_API_KEY is not set; returning a mock proposed action instead of calling OpenAI."
            );
            return Ok(mock_proposed_action(&request.input));
        }

        // reqwest is built without its own crypto provider, so use ring, which the
        // Temporal SDK already uses. This fails harmlessly if one is installed.
        let _ = rustls::crypto::ring::default_provider().install_default();

        // Let Temporal own retries: disable the HTTP client's own retry logic.
        let client = reqwest::Client::builder()
            .timeout(Duration::from_secs(30))
            .retry(reqwest::retry::never())
            .build()?;

        // Structured outputs: the API constrains the response to ProposedAction's
        // JSON schema. Strict mode requires every property to be listed as
        // required and additionalProperties to be false.
        let body = json!({
            "model": request.model,
            "instructions": request.instructions,
            "input": request.input,
            "text": {
                "format": {
                    "type": "json_schema",
                    "name": "proposed_action",
                    "strict": true,
                    "schema": {
                        "type": "object",
                        "properties": {
                            "action_type": { "type": "string" },
                            "description": { "type": "string" },
                            "reasoning": { "type": "string" },
                            "risky_action": { "type": "boolean" }
                        },
                        "required": ["action_type", "description", "reasoning", "risky_action"],
                        "additionalProperties": false
                    }
                }
            }
        });
        // An error here fails this attempt, so Temporal retries it.
        let response: serde_json::Value = client
            .post(RESPONSES_URL)
            .bearer_auth(api_key)
            .json(&body)
            .send()
            .await?
            .error_for_status()?
            .json()
            .await?;

        // Parse here, not in the Workflow: if the output is unusable, this Activity
        // attempt fails and Temporal retries the LLM call. Failing in Workflow code
        // would instead fail the Workflow Task and replay the same bad result.
        let text = output_text(&response);
        serde_json::from_str(&text).map_err(|err| {
            anyhow::anyhow!("LLM returned no parsable ProposedAction ({err}): {text:?}").into()
        })
    }

    /// Tells a human that an action is waiting for approval.
    ///
    /// This sample prints to the Worker's terminal. A real system might send an
    /// email, post to Slack, or update an approval queue UI instead.
    #[activity(name = "notify_approval_needed")]
    pub async fn notify_approval_needed(
        ctx: ActivityContext,
        request: ApprovalRequest,
    ) -> Result<(), ActivityError> {
        let workflow_id = ctx.info().workflow_id.clone().unwrap_or_default();
        let action = &request.proposed_action;
        println!("Approval needed for request: {}", request.request_id);

        let line = "=".repeat(60);
        println!(
            r#"
{line}
APPROVAL REQUIRED
{line}
Workflow ID: {workflow_id}
Request ID:  {request_id}
Action:      {action_type}
Description: {description}
Reasoning:   {reasoning}

To approve or reject, run:
  cargo run --bin send_approval -- approve "Looks good"
  cargo run --bin send_approval -- reject "Too risky"

Or with the Temporal CLI:
  temporal workflow signal --workflow-id {workflow_id} --name approval_decision --input '{{"approved": true}}'
{line}
"#,
            request_id = request.request_id,
            action_type = action.action_type,
            description = action.description,
            reasoning = action.reasoning,
        );
        Ok(())
    }

    /// Carries out the approved action.
    ///
    /// This sample only logs the action. A real system would call an external
    /// API, change a database, or invoke a tool the LLM selected.
    #[activity(name = "execute_action")]
    pub async fn execute_action(
        _ctx: ActivityContext,
        action: ProposedAction,
    ) -> Result<String, ActivityError> {
        println!("Executing action: {}", action.action_type);
        Ok(format!("Successfully executed: {}", action.action_type))
    }
}

/// Concatenates the `output_text` parts of the response's message items.
fn output_text(response: &serde_json::Value) -> String {
    response["output"]
        .as_array()
        .into_iter()
        .flatten()
        .filter(|item| item["type"] == "message")
        .flat_map(|item| item["content"].as_array().into_iter().flatten())
        .filter(|part| part["type"] == "output_text")
        .filter_map(|part| part["text"].as_str())
        .collect()
}
