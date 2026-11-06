// @@@SNIPSTART rust-quickstart-workflows
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityOptions, WorkflowContext, WorkflowContextView, WorkflowResult};
use std::time::Duration;

use crate::activities::MyActivities;

#[workflow]
pub struct GreetingWorkflow {
    name: String,
}

#[workflow_methods]
impl GreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());

        // Execute an activity
        let greeting = ctx
            .execute_activity(
                MyActivities::greet,
                name,
                ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
            )
            .await?;

        println!("{}", greeting);
        Ok(greeting)
    }
}
// @@@SNIPEND
