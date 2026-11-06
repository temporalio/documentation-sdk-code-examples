use std::time::Duration;

use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityOptions, WorkflowContext, WorkflowContextView, WorkflowResult};

use crate::activities::GreetingActivities;

/// Greets the name it was started with.
#[workflow]
pub struct HelloWorldWorkflow {
    name: String,
}

#[workflow_methods]
impl HelloWorldWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());
        let greeting = ctx
            .execute_activity(
                GreetingActivities::greet,
                name,
                ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
            )
            .await?;
        Ok(greeting)
    }
}

/// Takes no input and returns a fixed greeting.
#[workflow]
#[derive(Default)]
pub struct GreetingsWorkflow;

#[workflow_methods]
impl GreetingsWorkflow {
    #[run]
    pub async fn run(_ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        Ok("Hello, Temporal!".to_string())
    }
}
