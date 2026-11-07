use serde::{Deserialize, Serialize};
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{WorkflowContext, WorkflowResult};

#[derive(Debug, PartialEq, Serialize, Deserialize)]
pub struct ProcessingResult {
    pub status: String,
    pub records_processed: u32,
}

#[workflow]
#[derive(Default)]
pub struct ReturnValueWorkflow;

#[workflow_methods]
impl ReturnValueWorkflow {
    // @@@SNIPSTART rust-workflow-basics-return-values
    #[run]
    pub async fn run(_ctx: &mut WorkflowContext<Self>) -> WorkflowResult<ProcessingResult> {
        // Can return a complex result type
        let result = ProcessingResult {
            status: "completed".to_string(),
            records_processed: 100,
        };

        Ok(result)
    }
    // @@@SNIPEND
}
