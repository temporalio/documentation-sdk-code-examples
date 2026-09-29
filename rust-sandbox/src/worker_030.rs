use std::time::Duration;

use serde::{Serialize, Deserialize};
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{TimerOptions, WorkflowContext, WorkflowContextView, WorkflowResult};

#[derive(Serialize, Deserialize)]
pub struct ProcessingInput {
    pub data: Vec<String>,
    pub timeout_seconds: u32,
}

#[workflow]
pub struct ProcessingWorkflow {
    data: Vec<String>,
    timeout_seconds: u32,
}

#[workflow_methods]
impl ProcessingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, input: ProcessingInput) -> Self {
        Self {
            data: input.data,
            timeout_seconds: input.timeout_seconds,
        }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        // Good - deterministic timer
        ctx.timer(
            TimerOptions::builder(Duration::from_secs(60))
                .summary("important timer".to_string())
                .build()
        ).await;

        // Good - deterministic wait for condition
        ctx.wait_condition(|s| s.data.len() >= 3).await?;

        // Bad - nondeterministic sleep
        // tokio::time::sleep(Duration::from_secs(10)).await;

        // Bad - nondeterministic time
        // SystemTime::now()
        // Use the initialized state
        Ok("Processing complete".to_string())
    }
}