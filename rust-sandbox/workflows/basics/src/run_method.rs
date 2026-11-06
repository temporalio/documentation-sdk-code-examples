use std::time::Duration;
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityOptions, WorkflowContext, WorkflowContextView, WorkflowResult};

use crate::activities::MyActivities;

#[workflow]
pub struct RunMethodWorkflow {
    name: String,
}

#[workflow_methods]
impl RunMethodWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    // @@@SNIPSTART rust-workflow-basics-run-method
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());

        // Execute Activities, Timers, Child Workflows, and so on.
        let result = ctx
            .execute_activity(
                MyActivities::greet,
                name,
                ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
            )
            .await?;

        Ok(result)
    }
    // @@@SNIPEND
}
