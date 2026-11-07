use std::time::Duration;
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{SyncWorkflowContext, TimerOptions, WorkflowContext, WorkflowResult};

#[workflow]
#[derive(Default)]
pub struct DeterministicWorkflow {
    data: Vec<String>,
}

#[workflow_methods]
impl DeterministicWorkflow {
    // @@@SNIPSTART rust-workflow-basics-determinism
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        // Good - deterministic timer
        ctx.timer(
            TimerOptions::builder(Duration::from_secs(60))
                .summary("important timer".to_owned())
                .build(),
        )
        .await;

        // Good - deterministic wait for condition
        ctx.wait_condition(|s| s.data.len() >= 3).await?;

        // Bad - nondeterministic sleep
        // tokio::time::sleep(Duration::from_secs(10)).await;

        // Bad - nondeterministic time
        // SystemTime::now()

        Ok("Done".to_string())
    }
    // @@@SNIPEND

    #[signal]
    pub fn add_data(&mut self, _ctx: &mut SyncWorkflowContext<Self>, item: String) {
        self.data.push(item);
    }
}
