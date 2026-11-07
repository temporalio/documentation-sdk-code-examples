use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{WorkflowContext, WorkflowResult};

#[workflow]
#[derive(Default)]
pub struct GreetingsWorkflow;

#[workflow_methods]
impl GreetingsWorkflow {
    #[run]
    pub async fn run(_ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        Ok("Hello, Temporal!".to_string())
    }
}
