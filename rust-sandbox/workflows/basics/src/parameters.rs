use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{WorkflowContext, WorkflowContextView, WorkflowResult};

// @@@SNIPSTART rust-workflow-basics-parameters
use serde::{Serialize, Deserialize};

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
    pub async fn run(_ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        // Use the initialized state
        Ok("Processing complete".to_string())
    }
}
// @@@SNIPEND

impl ProcessingWorkflow {
    pub fn data(&self) -> &[String] {
        &self.data
    }

    pub fn timeout_seconds(&self) -> u32 {
        self.timeout_seconds
    }
}
