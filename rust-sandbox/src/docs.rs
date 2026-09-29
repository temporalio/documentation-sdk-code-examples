use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{WorkflowContext, WorkflowResult, WorkflowContextView};

#[workflow]
pub struct YourBasicWorkflow {
    param: String,
}

#[workflow_methods]
impl YourBasicWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, param: String) -> Self {
        Self { param }
    }

    #[run]
    pub async fn run(_ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        // ...
        Ok(String::new())
    }
}