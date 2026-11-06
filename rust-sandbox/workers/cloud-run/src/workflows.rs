use std::time::Duration;

use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityOptions, WorkflowContext, WorkflowContextView, WorkflowResult};

use crate::activities::MyActivities;

#[workflow]
pub struct MyWorkflow {
    name: String,
}

#[workflow_methods]
impl MyWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());
        let greeting = ctx
            .execute_activity(
                MyActivities::greet,
                name,
                ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
            )
            .await?;
        Ok(greeting)
    }
}
