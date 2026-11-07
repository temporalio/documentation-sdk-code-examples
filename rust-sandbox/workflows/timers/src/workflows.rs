use std::time::Duration;

use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{TimerOptions, WorkflowContext, WorkflowResult};

#[workflow]
#[derive(Default)]
pub struct TimerWorkflow;

#[workflow_methods]
impl TimerWorkflow {
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        // @@@SNIPSTART rust-timers-timer
        ctx.timer(
            TimerOptions::builder(Duration::from_secs(60))
                .summary("important timer".to_owned())
                .build(),
        )
        .await;
        // @@@SNIPEND

        Ok("Timer fired".to_string())
    }
}
