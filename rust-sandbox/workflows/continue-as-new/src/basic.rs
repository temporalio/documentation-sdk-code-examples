// @@@SNIPSTART rust-continue-as-new-basic
use std::time::Duration;

use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityOptions, ContinueAsNewOptions, WorkflowContext, WorkflowResult};

use crate::activities::MyActivities;

#[workflow]
#[derive(Default)]
pub struct GreetingWorkflow;

#[workflow_methods]
impl GreetingWorkflow {
    #[run(name = "greeting-workflow-1")]
    pub async fn run(ctx: &mut WorkflowContext<Self>, name: String) -> WorkflowResult<String> {
        let greeting = ctx
            .execute_activity(
                MyActivities::greet,
                name.clone(),
                ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
            )
            .await?;

        if name == "Ziggy" {
            Ok(greeting)
        } else {
            // continue_as_new() always returns an error that ends this run.
            // Propagate it with `?`. The empty match tells the compiler that
            // nothing runs after it.
            match ctx.continue_as_new("Ziggy".to_string(), ContinueAsNewOptions::default())? {}
        }
    }
}
// @@@SNIPEND
