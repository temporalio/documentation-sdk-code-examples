// @@@SNIPSTART rust-continue-as-new-test-hook
use std::time::Duration;

use serde::{Deserialize, Serialize};
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityOptions, ContinueAsNewOptions, WorkflowContext, WorkflowResult};

use crate::activities::MyActivities;

#[derive(Serialize, Deserialize)]
pub struct GreetingInput {
    pub name: String,
    pub greetings_remaining: u32,
    pub max_history_length: u32,
}

#[workflow]
#[derive(Default)]
pub struct RepeatGreetingWorkflow;

#[workflow_methods]
impl RepeatGreetingWorkflow {
    #[run]
    pub async fn run(
        ctx: &mut WorkflowContext<Self>,
        input: GreetingInput,
    ) -> WorkflowResult<String> {
        let mut greetings_remaining = input.greetings_remaining;

        while greetings_remaining > 0 {
            ctx.execute_activity(
                MyActivities::greet,
                input.name.clone(),
                ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
            )
            .await?;
            greetings_remaining -= 1;

            if greetings_remaining > 0
                && Self::should_continue_as_new(ctx, input.max_history_length)
            {
                // Pass the state the next run needs.
                let next_input = GreetingInput {
                    greetings_remaining,
                    ..input
                };
                match ctx.continue_as_new(next_input, ContinueAsNewOptions::default())? {}
            }
        }

        Ok(format!("Sent all greetings to {}", input.name))
    }

    fn should_continue_as_new(ctx: &WorkflowContext<Self>, max_history_length: u32) -> bool {
        if ctx.continue_as_new_suggested() {
            return true;
        }

        // For testing
        if max_history_length > 0 && ctx.history_length() > max_history_length {
            return true;
        }

        false
    }
}
// @@@SNIPEND
